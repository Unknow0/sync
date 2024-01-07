/**
 * 
 */
package unknow.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.ByteString;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import unknow.sync.proto.SyncMessage;

public class SendFile implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SendFile.class);

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
			SyncMessage m = new SyncMessage();
			LinkedBuffer b = LinkedBuffer.allocate();
			while ((l = in.read(buf)) > -1) {
				m.setData(ByteString.copyFrom(buf, 0, l));
				ProtobufIOUtil.writeDelimitedTo(out, m, SyncMessage.getSchema(), b);
			}
			m.setData(ByteString.EMPTY);
			ProtobufIOUtil.writeDelimitedTo(out, m, SyncMessage.getSchema(), b);
			out.flush();
		} catch (IOException e) {
			try {
				out.close();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			logger.warn("Failed to send", e);
		}
	}
}