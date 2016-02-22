package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class DeleteReq
	{
	public UUID uuid;
	public String file;

	public DeleteReq()
		{
		}

	public DeleteReq(UUID uuid, String file)
		{
		this.uuid=uuid;
		this.file=file;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
