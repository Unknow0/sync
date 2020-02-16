package unknow.sync.proto;

import unknow.sync.proto.pojo.UUID;

public class StartAppend {
	public UUID uuid;
	public String name;

	public StartAppend() {
	}

	public StartAppend(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	@Override
	public String toString() {
		return "StartAppend [uuid=" + uuid + ", name=" + name + "]";
	}
}
