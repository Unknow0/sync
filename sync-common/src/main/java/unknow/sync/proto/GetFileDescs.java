package unknow.sync.proto;

import java.util.Arrays;
import java.util.Collection;

import unknow.sync.proto.pojo.UUID;

public class GetFileDescs {
	public UUID uuid;
	public int[] fileId;

	public GetFileDescs() {
	}

	public GetFileDescs(UUID uuid, int[] fileId) {
		this.uuid = uuid;
		this.fileId = fileId;
	}

	public GetFileDescs(UUID uuid, Collection<Integer> fileId) {
		this.uuid = uuid;
		this.fileId = new int[fileId.size()];
		int i = 0;
		for (Integer v : fileId)
			this.fileId[i++] = v;
	}

	@Override
	public String toString() {
		return "GetFileDescs [uuid=" + uuid + ", fileId=" + Arrays.toString(fileId) + "]";
	}

}
