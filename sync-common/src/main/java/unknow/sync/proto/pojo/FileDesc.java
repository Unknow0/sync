package unknow.sync.proto.pojo;

public class FileDesc {
	public String name;
	public Bloc[] blocs;
	public Hash fileHash;

	public FileDesc() {
	}

	public FileDesc(String name, Bloc[] blocs, Hash fileHash) {
		this.name = name;
		this.blocs = blocs;
		this.fileHash = fileHash;
	}
}