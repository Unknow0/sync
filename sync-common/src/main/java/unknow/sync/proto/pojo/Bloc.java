package unknow.sync.proto.pojo;

import java.io.IOException;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * hash for a bloc of data
 * 
 * @author unknow
 */
public class Bloc {
	/** bloc's rolling crc */
	public int roll;
	/** bloc's hash */
	public long hash;

	/**
	 * create new Bloc
	 * 
	 * @param roll the rolling crc
	 * @param hash the hash
	 */
	public Bloc(int roll, long hash) {
		this.roll = roll;
		this.hash = hash;
	}

	/**
	 * unpack a bloc
	 * 
	 * @param unpacker the unpacker
	 * @throws IOException
	 */
	public Bloc(MessageUnpacker unpacker) throws IOException {
		this(unpacker.unpackInt(), unpacker.unpackLong());
	}

	/**
	 * @param packer
	 * @throws IOException
	 */
	public void write(MessagePacker packer) throws IOException {
		packer.packInt(roll);
		packer.packLong(hash);
	}
}
