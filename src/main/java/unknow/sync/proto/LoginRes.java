package unknow.sync.proto;

import unknow.common.tools.*;

public class LoginRes
	{
	public unknow.sync.proto.pojo.UUID uuid;
	public unknow.sync.proto.pojo.ProjectInfo project;

	public LoginRes()
		{
		}

	public LoginRes(unknow.sync.proto.pojo.UUID uuid, unknow.sync.proto.pojo.ProjectInfo project)
		{
		this.uuid=uuid;
		this.project=project;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
