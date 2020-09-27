package unknow.sync.proto.pojo;

import java.io.IOException;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class ProjectInfo {
	public int blocSize;
	public FileInfo[] files;

	public ProjectInfo(int blocSize, FileInfo[] files) {
		this.blocSize = blocSize;
		this.files = files;
	}

	public ProjectInfo(MessageUnpacker unpacker) throws IOException {
		blocSize = unpacker.unpackInt();
		int len = unpacker.unpackArrayHeader();
		files = new FileInfo[len];
		for (int i = 0; i < len; i++)
			files[i] = new FileInfo(unpacker);
	}

	public void write(MessagePacker packer) throws IOException {
		packer.packInt(blocSize);
		packer.packArrayHeader(files.length);
		for (int i = 0; i < files.length; i++)
			files[i].write(packer);
	}

	public static class FileInfo {
		public String name;
		public long size;
		public long hash;

		public FileInfo() {
		}

		public FileInfo(String name, long length, long hash) {
			this.name = name;
			this.size = length;
			this.hash = hash;
		}

		public FileInfo(MessageUnpacker unpacker) throws IOException {
			name = unpacker.unpackString();
			size = unpacker.unpackLong();
			hash = unpacker.unpackLong();
		}

		public void write(MessagePacker packer) throws IOException {
			packer.packString(name);
			packer.packLong(size);
			packer.packLong(hash);
		}

	}
}
