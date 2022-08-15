/**
 * 
 */
package unknow.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.protobuf.ByteString;

import unknow.sync.proto.SyncMessage;
import unknow.sync.proto.SyncMessage.Builder;

public class SendFile implements Runnable {

	private OutputStream out;
	private Path file;
	private long off;

	/**
	 * create new SendFile
	 * 
	 * @param out
	 * @param file
	 * @param off
	 */
	public SendFile(OutputStream out, Path file, long off) {
		this.out = out;
		this.file = file;
		this.off = off;
	}

	@Override
	public void run() {
		try (InputStream in = Files.newInputStream(file)) {
			in.skip(off);
			byte[] buf = new byte[131070]; // send bloc of 128K
			int l;
			Builder b = SyncMessage.newBuilder();
			while ((l = in.read(buf)) > -1)
				b.setData(ByteString.copyFrom(buf, 0, l)).build().writeDelimitedTo(out);
			b.setData(ByteString.EMPTY).build().writeDelimitedTo(out);
			out.flush();
		} catch (IOException e) {
		}
	}
}