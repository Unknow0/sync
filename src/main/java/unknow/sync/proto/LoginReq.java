package unknow.sync.proto;

import unknow.common.tools.*;
import unknow.sync.proto.pojo.*;

public class LoginReq
	{
	public String login;
	public String pass;
	public String project;
	public Action action;

	public LoginReq()
		{
		}
	public LoginReq(String login, String pass, String project, Action action)
		{
		this.login=login;
		this.pass=pass;
		this.project=project;
		this.action=action;
		}

	public String toString()
		{
		return JsonUtils.toString(this);
		}
	}
