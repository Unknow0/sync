package unknow.sync.proto;

import unknow.sync.proto.pojo.UUID;

public class AppendBloc {
	public UUID uuid;
	public int off;
	public int len;

	public AppendBloc() {
	}

	public AppendBloc(UUID uuid, int off, int len) {
		this.uuid = uuid;
		this.off = off;
		this.len = len;
	}

	@Override
	public String toString() {
		return "AppendBloc [uuid=" + uuid + ", off=" + off + ", len=" + len + "]";
	}
}
