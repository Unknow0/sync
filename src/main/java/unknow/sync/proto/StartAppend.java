package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class StartAppend
	{
	public UUID uuid;
	public String name;

	public StartAppend()
		{
		}

	public StartAppend(UUID uuid, String name)
		{
		this.uuid=uuid;
		this.name=name;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
