package unknow.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import unknow.sync.client.SyncRead;

/**
 * @author unknow
 */
public class Test {
	private static final Path TEST = Paths.get("test");
	private static final Path SRV = TEST.resolve("srv");
	private static final Path SRV_FILE = SRV.resolve("file");

	private static final Path CLIENT = TEST.resolve("client");
	private static final Path CLIENT_FILE = CLIENT.resolve("file");

	private static final Random rand = new Random();

	private static int size = 4096 + rand.nextInt(Short.MAX_VALUE);

	/**
	 * @param arg
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] arg) throws InterruptedException, IOException {
		System.out.println("file size: " + size);
		genFiles(SRV_FILE);
		Files.deleteIfExists(CLIENT_FILE);

//		Cfg cfg = new Cfg();
//		cfg.blocSize = 512;
//		cfg.path = SRV.toString();
//		cfg.port = 7777;
//		cfg.tokens = new HashSet<>(Arrays.asList("anonymous"));
//		new SyncServ(cfg);

		SyncRead syncRead = new SyncRead(CLIENT.toString(), "temp", "127.0.0.1", 7777);

		syncRead.process("anonymous", null);
		checkFiles("full download");

		changeFile(CLIENT_FILE, 1, 10);
		syncRead.process("anonymous", null);
		checkFiles("first bloc changed");

		changeFile(CLIENT_FILE, size - 50, 10);
		syncRead.process("anonymous", null);
		checkFiles("last bloc changed");

//		changeFile(CLIENT_FILE, cfg.blocSize + 20 + rand.nextInt(size - cfg.blocSize * 2), 20);
		syncRead.process("anonymous", null);
		checkFiles("middle bloc changed");

		genFiles(CLIENT_FILE);
		syncRead.process("anonymous", null);
		checkFiles("full file changed");
	}

	/**
	 * generate a random file
	 * 
	 * @param f
	 * @throws IOException
	 */
	public static void genFiles(Path f) throws IOException {
		Files.createDirectories(f.getParent());
		try (OutputStream out = Files.newOutputStream(f, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
			byte[] b = new byte[size];
			for (int i = 0; i < size; i++)
				b[i] = (byte) (rand.nextInt(256) & 0xFF);
			out.write(b);
		}
	}

	/**
	 * randomize data in file at off
	 * 
	 * @param f
	 * @param off
	 * @param len
	 * @throws IOException
	 */
	public static void changeFile(Path f, int off, int len) throws IOException {
		try (SeekableByteChannel out = Files.newByteChannel(f, StandardOpenOption.WRITE)) {
			out.position(off);
			ByteBuffer b = ByteBuffer.allocate(len);
			for (int i = 0; i < len; i++)
				b.put((byte) (rand.nextInt(256) & 0xFF));
			b.position(0);
			out.write(b);
		}
	}

	/**
	 * check if srv & client file have the same content
	 * 
	 * @param message
	 * @throws IOException
	 */
	public static void checkFiles(String message) throws IOException {
		try (InputStream is1 = Files.newInputStream(SRV_FILE); InputStream is2 = Files.newInputStream(CLIENT_FILE)) {
			byte[] b1 = new byte[4096];
			byte[] b2 = new byte[4096];

			int l1, o1 = 0;
			int l2, o2 = 0;
			do {
				l1 = is1.read(b1, o1, 4096 - o1);
				l2 = is2.read(b2, o2, 4096 - o2);

				int i;
				for (i = 0; i < Math.min(o1 + l1, o2 + l2); i++) {
					if (b1[i] != b2[i])
						throw new IOException("corrupt file in " + message);
				}
				o1 = compact(b1, i, l1);
				o2 = compact(b2, i, l2);
			} while (l1 == -1 && l2 == -1);
			if (o1 != o2 && o1 != 0)
				throw new IOException("corrupt file in " + message);
		}
	}

	/**
	 * @param b
	 * @param o
	 * @param l
	 * @return remaining data
	 */
	public static int compact(byte[] b, int o, int l) {
		if (o == l)
			return 0;
		l = b.length - o;
		System.arraycopy(b, o, b, 0, l);
		return l;
	}
}
