/**
 * 
 */
package unknow.sync.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import unknow.sync.common.pojo.FileDesc;
import unknow.sync.proto.BlocInfo;
import unknow.sync.proto.FileInfo;

/**
 * @author unknow
 */
public class FileUtils {
	/**
	 * path toString
	 * 
	 * @param root the root
	 * @param p    the file
	 * @return the path to string
	 */
	public static String toString(Path root, Path p) {
		Path relativize = root.relativize(p.toAbsolutePath());
		StringBuilder sb = new StringBuilder();
		Iterator<Path> it = relativize.iterator();
		sb.append(it.next().toString());
		while (it.hasNext()) {
			sb.append('/');
			sb.append(it.next().toString());
		}
		return sb.toString();
	}

	/**
	 * compute the hash of a file
	 * 
	 * @param root the root
	 * @param file the file path
	 * @return the file hash
	 * @throws IOException
	 */
	public static FileInfo fileInfo(Path root, Path file) throws IOException {
		FastHash checksum = new FastHash();

		try (InputStream fis = Files.newInputStream(file)) {
			long l = 0;
			byte[] buf = new byte[4096];
			int s;
			while ((s = fis.read(buf)) != -1) {
				checksum.update(buf, 0, s);
				l += s;
			}
			return FileInfo.newBuilder().setName(toString(root, file)).setSize(l).setHash(checksum.getValue()).build();
		}
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
		FastHash fileCheck = new FastHash();
		FastHash blocCheck = new FastHash();

		ArrayList<BlocInfo> list = new ArrayList<>();
		long size = 0;
		try (InputStream fis = Files.newInputStream(file)) {
			byte[] buf = new byte[Math.max(4096, blocSize)];
			int l = 0;
			int o = 0;
			while ((l = fis.read(buf, o, buf.length - o)) > 0) {
				size += l;
				fileCheck.update(buf, o, l);
				l += o;
				o = 0;
				while (l >= blocSize) {
					blocCheck.reset();
					blocCheck.update(buf, o, blocSize);
					list.add(BlocInfo.newBuilder().setRoll(RollingChecksum.compute(buf, o, blocSize)).setHash(blocCheck.getValue()).build());
					l -= blocSize;
					o += blocSize;
				}
				System.arraycopy(buf, o, buf, 0, l);
				o = l;
			}
			// padding
			if (o > 0) {
				byte p = 0;
				while (o < blocSize)
					buf[o++] = ++p;
				blocCheck.reset();
				blocCheck.update(buf, 0, blocSize);
				list.add(BlocInfo.newBuilder().setRoll(RollingChecksum.compute(buf, 0, blocSize)).setHash(blocCheck.getValue()).build());
			}
		}
		list.trimToSize();
		return new FileDesc(toString(root, file), list, fileCheck.getValue(), size);
	}
}
