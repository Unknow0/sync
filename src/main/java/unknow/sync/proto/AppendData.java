package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class AppendData
	{
	public UUID uuid;
	public byte[] data;

	public AppendData()
		{
		}

	public AppendData(UUID uuid, byte[] data)
		{
		this.uuid=uuid;
		this.data=data;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
