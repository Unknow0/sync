package unknow.sync.server;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.EnumMap;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import unknow.sync.Query;
import unknow.sync.proto.pojo.Bloc;
import unknow.sync.proto.pojo.FileDesc;
import unknow.sync.proto.pojo.ProjectInfo;

public class SyncHandler extends ChannelHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(SyncHandler.class);
	private static final Query[] QUERY = Query.values();

	private Project serv;
	private boolean logged = false;

	private final EnumMap<Query, QueryHandler> handlers = new EnumMap<>(Query.class);

	public SyncHandler(Project serv) {
		this.serv = serv;

		handlers.put(Query.LOGIN, (u, ctx) -> {
			String token = u.unpackString();
			log.trace("	({}, {})", token);

			if (!serv.asRight(token))
				throw new IOException("login failed");
			logged = true;

			ProjectInfo projectInfo = serv.projectInfo();

			ByteBuf out = ctx.alloc().buffer();
			MessagePacker pack = MessagePack.newDefaultPacker(new ByteBufOutputStream(out));
			projectInfo.write(pack);
			pack.flush();
			ctx.writeAndFlush(out);
		});

		handlers.put(Query.GETFILE, (u, ctx) -> {
			String file = u.unpackString();
			long off = u.unpackLong();
			log.trace("	({}, {})", file, off);

			if (!logged)
				throw new IOException("invalide state");

			File f = serv.file(file).toFile();
			if (!f.isFile())
				throw new IOException("invalid file");

			new SendFile(ctx.channel(), f, off).start();
		});

		handlers.put(Query.FILEBLOC, (u, ctx) -> {
			String file = u.unpackString();
			log.trace("	({})", file);

			if (!logged)
				throw new IOException("invalide state");

			FileDesc fileDesc = serv.fileDesc(file);
			if (fileDesc == null)
				throw new IOException("invalid file");

			Bloc[] b = fileDesc.blocs;
			MessageBufferPacker p = MessagePack.newDefaultBufferPacker();
			p.packArrayHeader(b.length);
			for (int i = 0; i < b.length; i++)
				b[i].write(p);
			ctx.writeAndFlush(Unpooled.wrappedBuffer(p.toByteArray()));
		});

		handlers.put(Query.GETBLOC, (u, ctx) -> {
			String file = u.unpackString();
			int off = u.unpackInt();
			log.trace("	({}, {})", file, off);

			if (!logged)
				throw new IOException("invalide state");
			File f = serv.file(file).toFile();
			if (!f.isFile())
				throw new IOException("invalid file");

			MessageBufferPacker p = MessagePack.newDefaultBufferPacker();
			try (RandomAccessFile ram = new RandomAccessFile(f, "r")) {
				int len = serv.blocSize();
				byte[] b = new byte[len]; // send bloc of 128K
				ram.seek(off * len);
				int n = 0;
				do {
					int count = ram.read(b, n, len - n);
					if (count < 0)
						break;
					n += count;
				} while (n < len);
				p.packBinaryHeader(n);
				p.addPayload(b, 0, n);
				ctx.writeAndFlush(Unpooled.wrappedBuffer(p.toByteArray()));
			}
		});
	}

	ByteBuf remaining;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf) msg;
		if (remaining != null) {
			buf = Unpooled.wrappedBuffer(remaining, buf);
			remaining = null;
		}
		MessageUnpacker u = MessagePack.newDefaultUnpacker(new ByteBufInputStream(buf));
		try {
			int qInt = u.unpackInt();
			Query q = QUERY[qInt];
			log.info("{} {}", ctx.channel(), q);
			handlers.get(q).handle(u, ctx);
			buf.resetReaderIndex();
			buf.skipBytes((int) u.getTotalReadBytes());
		} catch (MessageInsufficientBufferException e) {
			buf.resetReaderIndex();
			remaining = buf.copy();
		} catch (Exception e) {
			log.error("", e);
			ctx.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("", cause);
		ctx.close();
	}

	private interface QueryHandler {
		void handle(MessageUnpacker u, ChannelHandlerContext ctx) throws IOException;
	}

	private static class SendFile extends Thread {
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
}
