package unknow.sync.proto;

import unknow.sync.proto.pojo.UUID;

public class GetFileReq {
	public UUID uuid;
	public String name;
	public long off;

	public GetFileReq() {
	}

	public GetFileReq(UUID uuid, String name, long off) {
		this.uuid = uuid;
		this.name = name;
		this.off = off;
	}

	@Override
	public String toString() {
		return "GetFileReq [uuid=" + uuid + ", name=" + name + ", off=" + off + "]";
	}
}
