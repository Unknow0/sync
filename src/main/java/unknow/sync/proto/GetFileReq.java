package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class GetFileReq
	{
	public UUID uuid;
	public String name;
	public long off;

	public GetFileReq()
		{
		}

	public GetFileReq(UUID uuid, String name, long off)
		{
		this.uuid=uuid;
		this.name=name;
		this.off=off;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
