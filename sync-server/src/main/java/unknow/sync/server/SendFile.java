/**
 * 
 */
package unknow.sync.server;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

class SendFile implements Runnable {
	private static final ExecutorService POOL = Executors.newCachedThreadPool();
	private Channel ctx;
	private File file;
	private long off;

	/**
	 * create new SendFile
	 * 
	 * @param ctx
	 * @param file
	 * @param off
	 */
	public SendFile(Channel ctx, File file, long off) {
		super();
		this.ctx = ctx;
		this.file = file;
		this.off = off;
		POOL.execute(this);
	}

	@Override
	public void run() {
		MessageBufferPacker p = MessagePack.newDefaultBufferPacker();

		try (RandomAccessFile ram = new RandomAccessFile(file, "r")) {
			byte[] buf = new byte[131070]; // send bloc of 128K
			ram.seek(off);
			int l;
			while ((l = ram.read(buf)) > -1) {
				p.packBinaryHeader(l);
				p.addPayload(buf, 0, l);
				p.flush();
				ctx.writeAndFlush(Unpooled.wrappedBuffer(p.toByteArray())).awaitUninterruptibly();
				p.clear();
			}
			p.packNil();
			ctx.writeAndFlush(Unpooled.wrappedBuffer(p.toByteArray()));
		} catch (IOException e) {
		}
	}
}