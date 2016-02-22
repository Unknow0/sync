package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class AppendBloc
	{
	public UUID uuid;
	public int off;
	public int len;

	public AppendBloc()
		{
		}

	public AppendBloc(UUID uuid, int off, int len)
		{
		this.uuid=uuid;
		this.off=off;
		this.len=len;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
