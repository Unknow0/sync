package unknow.sync.common.pojo;

import java.io.IOException;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * info of the project
 * 
 * @author unknow
 */
public class ProjectInfo {
	/** size of blocs */
	public int blocSize;
	/** all file */
	public FileInfo[] files;

	/**
	 * create new ProjectInfo
	 * 
	 * @param blocSize
	 * @param files
	 */
	public ProjectInfo(int blocSize, FileInfo[] files) {
		this.blocSize = blocSize;
		this.files = files;
	}

	/**
	 * create new ProjectInfo
	 * 
	 * @param unpacker
	 * @throws IOException
	 */
	public ProjectInfo(MessageUnpacker unpacker) throws IOException {
		blocSize = unpacker.unpackInt();
		int len = unpacker.unpackArrayHeader();
		files = new FileInfo[len];
		for (int i = 0; i < len; i++)
			files[i] = new FileInfo(unpacker);
	}

	/**
	 * @param packer
	 * @throws IOException
	 */
	public void write(MessagePacker packer) throws IOException {
		packer.packInt(blocSize);
		packer.packArrayHeader(files.length);
		for (int i = 0; i < files.length; i++)
			files[i].write(packer);
	}
}
