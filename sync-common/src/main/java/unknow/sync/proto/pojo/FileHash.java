package unknow.sync.proto.pojo;

public class FileHash {
	public String name;
	public Hash hash;

	public FileHash() {
	}

	public FileHash(String name, Hash hash) {
		this.name = name;
		this.hash = hash;
	}
}
