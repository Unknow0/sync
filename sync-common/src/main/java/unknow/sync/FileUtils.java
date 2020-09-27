/**
 * 
 */
package unknow.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import unknow.sync.proto.pojo.Bloc;
import unknow.sync.proto.pojo.FileDesc;
import unknow.sync.proto.pojo.ProjectInfo.FileInfo;

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
		Path relativize = p.toAbsolutePath().relativize(root);
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
			return new FileInfo(toString(root, file), l, checksum.getValue());
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

		FileDesc desc = new FileDesc(toString(root, file));
		List<Bloc> list = new ArrayList<>();
		long size = 0;
		try (InputStream fis = Files.newInputStream(file)) {
			byte[] buf = new byte[blocSize];
			int l = 0;
			while ((l = fis.read(buf, 0, blocSize)) > 0) {
				int count;
				while (l < blocSize && (count = fis.read(buf, l, blocSize - l)) > 0) // finish to read the bloc
					l += count;

				blocCheck.reset();
				size += l;
				fileCheck.update(buf, 0, l);

				// padding
				byte p = 0;
				while (l < buf.length)
					buf[l++] = ++p;
				blocCheck.update(buf, 0, l);
				list.add(new Bloc(RollingChecksum.compute(buf, 0, l), blocCheck.getValue()));
			}
		}
		desc.hash = fileCheck.getValue();
		desc.blocs = list.toArray(new Bloc[0]);
		desc.size = size;
		return desc;
	}
}
