/**
 * 
 */
package unknow.sync.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.sync.common.FastHash;
import unknow.sync.common.FileUtils;
import unknow.sync.common.RollingChecksum;
import unknow.sync.common.pojo.Bloc;
import unknow.sync.common.pojo.FileDesc;
import unknow.sync.common.pojo.FileInfo;
import unknow.sync.common.pojo.ProjectInfo;

/**
 * update local file
 * 
 * @author unknow
 */
public class SyncRead {
	private static final Logger log = LoggerFactory.getLogger(SyncRead.class);

	private SyncConnection client;
	private SyncListener listener = new SyncListener.Log();

	private Path root;
	private Path tmp;
	private int blocSize;
	private Set<String> allFiles = new HashSet<>();

	private final FastHash md = new FastHash();
	private final byte[] buf = new byte[4096];

	private long byteTotal = 0;
	private long byteDone = 0;

	/**
	 * create new SyncRead
	 * 
	 * @param root
	 * @param tmp
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public SyncRead(String root, String tmp, String host, int port) throws IOException {
		this.root = Paths.get(root).toAbsolutePath();
		this.tmp = Paths.get(tmp).toAbsolutePath();

		client = new SyncConnection(host, port);
	}

	/**
	 * @param listener the listener to use
	 */
	public void setListener(SyncListener listener) {
		this.listener = listener;
	}

	/**
	 * do the work
	 * 
	 * @param token token to use for login
	 * @param regex files to excludes
	 * @throws IOException
	 */
	public void process(String token, Pattern regex) throws IOException {
		listener.start();

		Matcher m = regex == null ? null : regex.matcher("");

		ProjectInfo info = client.login(token);
		blocSize = info.blocSize;

		FileInfo[] files = info.files;
		int l = files.length;
		for (int i = 0; i < l; i++) {
			FileInfo fileInfo = files[i];
			if (m != null) {
				m.reset(fileInfo.name);
				if (m.matches()) {
					log.warn("excluding '{}'", fileInfo.name);
					continue;
				}
			}
			allFiles.add(fileInfo.name);
			byteTotal += fileInfo.size;
		}
		listener.update(byteDone, byteTotal);

		for (int i = 0; i < l; i++) {
			FileInfo fileInfo = files[i];
			if (allFiles.contains(fileInfo.name))
				process(new ToProcess(root.resolve(fileInfo.name).toAbsolutePath(), fileInfo));
		}

		log.debug("removing extra file");
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (tmp.equals(dir))
					return FileVisitResult.SKIP_SUBTREE;
				if (m == null)
					return FileVisitResult.CONTINUE;
				m.reset(FileUtils.toString(root, dir));
				return m.matches() ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String string = FileUtils.toString(root, file);
				if (m != null) {
					m.reset(string);
					if (m.matches())
						return FileVisitResult.CONTINUE;
				}
				if (!allFiles.contains(string))
					Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});

		if (Files.exists(tmp)) {
			log.debug("cleanup tmp");
			Files.walkFileTree(tmp, new SimpleFileVisitor<Path>() {
				private Deque<Integer> saw = new LinkedList<>();

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					saw.addFirst(0);
					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					int i = saw.removeFirst();
					saw.addFirst(i + 1);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (0 == saw.removeFirst())
						Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}

		listener.done(byteDone);
	}

	private static class ToProcess {
		public FileInfo remote;
		public FileInfo local;
		public Path localFile;
		public FileDesc tmp;
		public Path tmpFile;

		public ToProcess(Path localFile, FileInfo remote) {
			this.localFile = localFile;
			this.remote = remote;
		}
	}

	/**
	 * load local fileDesc
	 * 
	 * @throws IOException
	 */
	private void process(ToProcess t) throws IOException {
		log.debug("processing '{}'", t.remote.name);
		boolean exists = Files.exists(t.localFile);

		// check local file first
		if (exists) {
			FileInfo remote = t.remote;
			t.local = FileUtils.fileInfo(root, t.localFile);
			if (remote.hash == t.local.hash && remote.size == t.local.size) {
				byteDone += t.remote.size;
				listener.update(byteDone, byteTotal);
				return;
			}
		}

		// if local file has changed load tmp file
		t.tmpFile = tmp(t.remote.name);
		if (Files.exists(t.tmpFile)) {
			try {
				t.tmp = FileUtils.loadFile(tmp, t.tmpFile, blocSize);
				if (t.tmp.size > t.remote.size)
					t.tmp = null; // invalid temporary file
			} catch (IOException e) {
			}
		}

		if (exists)
			update(t);
		else
			download(t);
	}

	private Path tmp(String name) throws IOException {
		Path tmpFile = tmp.resolve(name);
		Files.createDirectories(tmpFile.getParent());
		return tmpFile;
	}

	private static void moveTmp(ToProcess t) throws IOException {
		Files.createDirectories(t.localFile.getParent());
		Files.move(t.tmpFile, t.localFile, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * download the full file
	 * 
	 * @param t
	 * @throws IOException
	 */
	private void download(ToProcess t) throws IOException {
		log.debug("downloading '{}'", t.remote.name);
		md.reset();
		OpenOption options = StandardOpenOption.CREATE;
		long off = 0;
		if (t.tmp != null) { // assuming the existing data are valid
			options = StandardOpenOption.APPEND;
			off = t.tmp.size;
			// update byteDone & hash from tmp files
			try (InputStream in = Files.newInputStream(t.tmpFile)) {
				int l;
				byte[] buf = new byte[4096];
				while ((l = in.read(buf)) > 0) {
					md.update(buf, 0, l);
					byteDone += l;
					listener.update(byteDone, byteTotal);
				}
			}
		}
		long before = byteDone;
		try (OutputStream fos = Files.newOutputStream(t.tmpFile, options)) {
			client.getFile(t.remote.name, off, b -> {
				byteDone += b.length;
				fos.write(b);
				md.update(b);
				listener.update(byteDone, byteTotal);
			});
		}
		long digest = md.getValue();
		if (digest != t.remote.hash) {
			byteDone = before;
			// TODO
			log.warn("check failed for '{}'", t.remote.name);
		} else
			moveTmp(t);
	}

	/**
	 * update file with bloc diff
	 * 
	 * @param t
	 * @throws IOException
	 */
	private void update(ToProcess t) throws IOException {
		log.debug("updating '{}'", t.remote.name);
		md.reset();
		long before = byteDone;

		BlocToProcess[] blocs = client.fileBlocs(t.remote.name);

		int off = 0;
		if (t.tmp != null) { // checking temporary file
			Bloc[] tmpBlocs = t.tmp.blocs;
			while (off < tmpBlocs.length && tmpBlocs[off].equals(blocs[off]))
				off++;
			if (off == blocs.length && t.tmp.size == t.remote.size && t.tmp.hash == t.remote.hash) { // tmp is the full file
				byteDone += t.tmp.size;
				moveTmp(t);
				listener.update(byteDone, byteTotal);
				return;
			}
			if (off == tmpBlocs.length && t.tmp.size % blocSize != 0) // last bloc is a false positive
				off--;
			if (off > 0)
				off--;

			log.debug("found {} bloc in tmp", off);
			if (off > 0) {
				long end = off * blocSize;
				try (InputStream in = Files.newInputStream(t.localFile)) {
					int l = 0;
					while (end > 0 && (l = in.read(buf)) > 0) {
						md.update(buf, 0, l);
						end -= l;
					}
				}
			}
		}

		try (RandomAccessFile out = new RandomAccessFile(t.tmpFile.toFile(), "rw"); RandomAccessFile local = new RandomAccessFile(t.localFile.toFile(), "r")) {
			findBlocs(local, blocs, off, t.remote.size);

			// reconstruct file
			if (off > 0)
				out.seek(off * blocSize);
			long last = off * blocSize;
			for (; off < blocs.length; off++) {
				List<Long> found = blocs[off].found;
				if (!found.isEmpty()) { // bloc found in local file
					long o = found.get(0);
					for (Long f : found) {
						if (f == last) {
							o = f;
							break;
						}
					}
					local.seek(o);
					last = o;
					int r = blocSize;
					int l;
					while (r > 0 && (l = local.read(buf, 0, Math.min(buf.length, r))) > 0) {
						md.update(buf, 0, l);
						out.write(buf, 0, l);
						r -= l;
						byteDone += l;
						last += l;
					}
					listener.update(byteDone, byteTotal);
				} else {
					byte[] bloc = client.getBloc(t.remote.name, off);
					out.write(bloc);
					md.update(bloc);
					byteDone += bloc.length;
					listener.update(byteDone, byteTotal);
					last += bloc.length;
				}
			}
		}

		long digest = md.getValue();
		if (digest != t.remote.hash) {
			byteDone = before;
			listener.update(byteDone, byteTotal);

			// TODO
			log.warn("check failed for '{}'", t.remote.name);
		} else
			moveTmp(t);
	}

	private void findBlocs(RandomAccessFile local, BlocToProcess[] blocs, int off, long size) throws IOException {
		RollingChecksum crc = new RollingChecksum(blocSize);
		FastHash h = new FastHash();

		Map<Integer, List<BlocToProcess>> remote = new HashMap<>();
		for (int i = off; i < blocs.length; i++) {
			BlocToProcess b = blocs[i];
			List<BlocToProcess> list = remote.get(b.roll);
			if (list == null)
				remote.put(b.roll, list = new ArrayList<>());
			list.add(b);
		}

		int l;
		// read local & find existing blocs
		while ((l = local.read(buf)) > 0) {
			for (int i = 0; i < l; i++) {
				int roll = crc.append(buf[i]);
				List<BlocToProcess> list = remote.get(roll);
				if (list == null)
					continue;
				h.reset();
				h.update(crc.buf());
				long value = h.getValue();
				long o = local.getFilePointer() - l + i + 1 - blocSize;
				for (BlocToProcess b : list) {
					if (b.hash == value)
						b.found.add(o);
				}
			}
		}
		// add padding to find last bloc
		BlocToProcess lastBloc = blocs[blocs.length - 1];
		byte b = 0;
		int lastBlocSize = (int) (size % blocSize);
		long o = local.getFilePointer() - lastBlocSize;
		for (int i = 0; i < blocSize; i++) {
			int roll = crc.append(++b);
			if (roll != lastBloc.roll)
				continue;
			h.reset();
			h.update(crc.buf());
			long value = h.getValue();
			if (lastBloc.hash == value)
				lastBloc.found.add(o);
		}
	}
}
