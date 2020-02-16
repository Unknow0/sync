package unknow.sync.proto;

import unknow.sync.proto.pojo.Action;

public class LoginReq {
	public String login;
	public String pass;
	public String project;
	public Action action;

	public LoginReq() {
	}

	public LoginReq(String login, String pass, String project, Action action) {
		this.login = login;
		this.pass = pass;
		this.project = project;
		this.action = action;
	}

	@Override
	public String toString() {
		return "LoginReq [login=" + login + ", pass=" + pass + ", project=" + project + ", action=" + action + "]";
	}

}
