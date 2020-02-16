package unknow.sync;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class KryoDecoder extends ByteToMessageDecoder {
	private static final Logger log = LoggerFactory.getLogger(KryoDecoder.class);
	private Kryos kryos;

	public KryoDecoder(Kryos kryos) {
		this.kryos = kryos;
	}

	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() < 1)
			return;
		try {
			byte[] dst = new byte[in.readableBytes()];
			in.getBytes(in.readerIndex(), dst);

			Input input = new Input(dst);

			Object o = kryos.read(input);
			out.add(o);
			log.trace("{}", o);

			in.skipBytes(input.position());
		} catch (KryoException e) {
			if (!e.getMessage().contains("Buffer underflow")) {
				System.out.println("message '" + e.getMessage() + "'");
				e.printStackTrace();
			}
		}
	}
}
