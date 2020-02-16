package unknow.sync.proto;

import unknow.sync.proto.pojo.Hash;
import unknow.sync.proto.pojo.UUID;

public class EndAppend {
	public UUID uuid;
	public Hash hash;

	public EndAppend() {
	}

	public EndAppend(UUID uuid, Hash hash) {
		this.uuid = uuid;
		this.hash = hash;
	}

	@Override
	public String toString() {
		return "EndAppend [uuid=" + uuid + ", hash=" + hash + "]";
	}

}
