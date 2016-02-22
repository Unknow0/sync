package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class GetBlocReq
	{
	public UUID uuid;
	public String name;
	public int off;
	public int len;

	public GetBlocReq()
		{
		}

	public GetBlocReq(UUID uuid, String name, int off, int len)
		{
		this.uuid=uuid;
		this.name=name;
		this.off=off;
		this.len=len;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
