/**
 * 
 */
package unknow.sync.common.pojo;

import java.io.IOException;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * info of a file
 * 
 * @author unknow
 */
public class FileInfo {
	/** file name */
	public String name;
	/** file size */
	public long size;
	/** file hash */
	public long hash;

	/**
	 * create new FileInfo
	 * 
	 * @param name
	 * @param length
	 * @param hash
	 */
	public FileInfo(String name, long length, long hash) {
		this.name = name;
		this.size = length;
		this.hash = hash;
	}

	/**
	 * create new FileInfo
	 * 
	 * @param unpacker
	 * @throws IOException
	 */
	public FileInfo(MessageUnpacker unpacker) throws IOException {
		name = unpacker.unpackString();
		size = unpacker.unpackLong();
		hash = unpacker.unpackLong();
	}

	/**
	 * @param packer
	 * @throws IOException
	 */
	public void write(MessagePacker packer) throws IOException {
		packer.packString(name);
		packer.packLong(size);
		packer.packLong(hash);
	}

}