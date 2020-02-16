package unknow.sync.proto;

import java.util.Arrays;

import unknow.sync.proto.pojo.UUID;

public class AppendData {
	public UUID uuid;
	public byte[] data;

	public AppendData() {
	}

	public AppendData(UUID uuid, byte[] data) {
		this.uuid = uuid;
		this.data = data;
	}

	@Override
	public String toString() {
		return "AppendData [uuid=" + uuid + ", data=" + Arrays.toString(data) + "]";
	}
}
