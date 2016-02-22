package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class EndAppend
	{
	public UUID uuid;
	public Hash hash;

	public EndAppend()
		{
		}

	public EndAppend(UUID uuid, Hash hash)
		{
		this.uuid=uuid;
		this.hash=hash;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
