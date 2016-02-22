package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class GetFileDescs
	{
	public UUID uuid;
	public int[] fileId;

	public GetFileDescs()
		{
		}

	public GetFileDescs(UUID uuid, int[] fileId)
		{
		this.uuid=uuid;
		this.fileId=fileId;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
