package unknow.sync.proto;

import unknow.sync.proto.pojo.UUID;

public class DeleteReq {
	public UUID uuid;
	public String file;

	public DeleteReq() {
	}

	public DeleteReq(UUID uuid, String file) {
		this.uuid = uuid;
		this.file = file;
	}

	@Override
	public String toString() {
		return "DeleteReq [uuid=" + uuid + ", file=" + file + "]";
	}

}
