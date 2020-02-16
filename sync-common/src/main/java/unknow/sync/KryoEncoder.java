package unknow.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class KryoEncoder extends MessageToByteEncoder<Object> {
	private static final Logger log = LoggerFactory.getLogger(KryoDecoder.class);
	private Kryos kryos;

	public KryoEncoder(Kryos kryos) {
		this.kryos = kryos;
	}

	protected void encode(ChannelHandlerContext ctx, Object data, ByteBuf out) throws Exception {
		ByteBufOutputStream buf = new ByteBufOutputStream(out);
		Output output = new Output(buf);
		kryos.write(output, data);
		output.close();
		log.trace("{}", data);
	}
}