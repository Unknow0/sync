package unknow.sync.proto.pojo;

public class FileDesc {
	public String name;
	public Bloc[] blocs;
	public long hash;
	public long size;

	public FileDesc() {
	}

	public FileDesc(String name) {
		this.name = name;
	}
}  