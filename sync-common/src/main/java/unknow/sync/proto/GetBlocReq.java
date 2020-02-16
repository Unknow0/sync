package unknow.sync.proto;

import unknow.sync.proto.pojo.UUID;

public class GetBlocReq {
	public UUID uuid;
	public String name;
	public int off;
	public int len;

	public GetBlocReq() {
	}

	public GetBlocReq(UUID uuid, String name, int off, int len) {
		this.uuid = uuid;
		this.name = name;
		this.off = off;
		this.len = len;
	}

	@Override
	public String toString() {
		return "GetBlocReq [uuid=" + uuid + ", name=" + name + ", off=" + off + ", len=" + len + "]";
	}

}
