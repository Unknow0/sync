package unknow.sync;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import unknow.sync.proto.pojo.Bloc;
import unknow.sync.proto.pojo.FileDesc;
import unknow.sync.proto.pojo.Hash;

/**
 * utilities to manage FileDesc
 * 
 * @author unknow
 */
public class FileDescLoader {

	/**
	 * load all file desc
	 * 
	 * @param files    output
	 * @param root     root to look for file
	 * @param blocSize bloc size for file desc
	 * @param pattern  only matching file will be loaded
	 * @throws IOException
	 */
	public static void load(Collection<FileDesc> files, Path root, int blocSize, Pattern pattern) throws IOException {
		root = root.toAbsolutePath().normalize();
		Files.walkFileTree(root, new Visitor(root, blocSize, pattern, files::add));
	}

	/**
	 * find matching bloc in two file
	 * 
	 * @param a file a
	 * @param b file b
	 * @return matching bloc B bloc -&gt; A bloc
	 */
	public static Map<Integer, Integer> diff(FileDesc a, FileDesc b) {
		Map<Integer, List<IndexedHash>> blocA = new HashMap<>();
		int i = 0;
		for (Bloc bloc : a.blocs) {
			List<IndexedHash> l = blocA.get(bloc.roll);
			if (l == null) {
				l = new ArrayList<>();
				blocA.put(bloc.roll, l);
			}
			l.add(new IndexedHash(i++, bloc.hash));
		}

		Map<Integer, Integer> map = new HashMap<>();
		loop: for (i = 0; i < b.blocs.length; i++) {
			Bloc blocB = b.blocs[i];
			List<IndexedHash> list = blocA.get(blocB.roll);
			if (list != null) {
				for (IndexedHash bloc : list) {
					if (blocB.hash.equals(bloc.h)) {
						map.put(i, bloc.i);
						continue loop;
					}
				}
			}
		}
		return map;
	}

	/**
	 * load a filedesc
	 * 
	 * @param root     root path
	 * @param file     the file path
	 * @param blocSize the size of blocs
	 * @return the file desc
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static FileDesc loadFile(Path root, Path file, int blocSize) throws FileNotFoundException, IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			MessageDigest fileMd = MessageDigest.getInstance("SHA-512");

			FileDesc desc = new FileDesc(toString(file), null, null);
			List<Bloc> list = new ArrayList<>();
			try (FileInputStream fis = new FileInputStream(root.resolve(file).toString())) {
				byte[] buf = new byte[blocSize];
				byte[] h;
				int s;
				while ((s = fis.read(buf)) != -1) {
					int c;
					while (s < buf.length && (c = fis.read()) != -1)
						buf[s++] = (byte) c;
					fileMd.update(buf, 0, s);

					// padding
					byte p = 0;
					while (s < buf.length)
						buf[s++] = ++p;
					h = md.digest(buf);
					Bloc bloc = new Bloc(RollingChecksum.compute(buf), new Hash(h));
					list.add(bloc);
				}
				h = fileMd.digest();
				desc.fileHash = new Hash(h);
				desc.blocs = list.toArray(new Bloc[0]);
			}
			return desc;
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	/**
	 * path toString
	 * 
	 * @param p
	 * @return the path to string
	 */
	public static String toString(Path p) {
		StringBuilder sb = new StringBuilder();
		Iterator<Path> it = p.iterator();
		sb.append(it.next().toString());
		while (it.hasNext()) {
			sb.append('/');
			sb.append(it.next().toString());
		}
		return sb.toString();
	}

	private static class Visitor implements FileVisitor<Path> {
		private Path root;
		private int blocSize;
		private Matcher m;
		private Consumer<FileDesc> c;

		public Visitor(Path root, int blocFile, Pattern pattern, Consumer<FileDesc> c) {
			this.root = root;
			this.blocSize = blocFile;
			this.m = pattern == null ? null : pattern.matcher("");
			this.c = c;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (m != null)
				m.reset(root.relativize(dir).toString());
			return m == null || m.matches() || m.hitEnd() ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (attrs.isRegularFile()) {
				if (m != null)
					m.reset(root.relativize(file).toString());
				if (m == null || m.matches())
					c.accept(loadFile(root, root.relativize(file), blocSize));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	/**
	 * id -&gt; hash
	 * 
	 * @author unknow
	 */
	public static class IndexedHash {
		/** the id */
		public int i;
		/** the hash */
		public Hash h;

		/**
		 * create new IndexedHash
		 * 
		 * @param i the id
		 * @param h the hash
		 */
		public IndexedHash(int i, Hash h) {
			this.i = i;
			this.h = h;
		}
	}
}
