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
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import unknow.sync.common.Query;
import unknow.sync.common.pojo.Bloc;
import unknow.sync.common.pojo.FileDesc;
import unknow.sync.common.pojo.ProjectInfo;

/**
 * handle sync request
 * 
 * @author unknow
 */
public class SyncHandler extends ChannelHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(SyncHandler.class);
	private static final Query[] QUERY = Query.values();

	private boolean logged = false;

	private final EnumMap<Query, QueryHandler> handlers = new EnumMap<>(Query.class);

	/**
	 * create new SyncHandler
	 * 
	 * @param serv
	 */
	public SyncHandler(Project serv) {
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

			new SendFile(ctx.channel(), f, off);
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
				byte[] b = new byte[len];
				ram.seek(off * len);
				int n = 0;
				do {
					int count = ram.read(b, n, len - n);
					if (count < 0)
						break;
					n += count;
				} while (n < len);

				if (log.isTraceEnabled()) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < n;) {
						System.out.format("%02x", b[i++] & 0xFF);
						if (i % 32 == 0) {
							log.trace("{}", sb);
							sb.setLength(0);
						} else if (i % 4 == 0)
							sb.append(' ');
					}
					if (sb.length() > 0)
						log.trace("{}", sb);
				}

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
}
