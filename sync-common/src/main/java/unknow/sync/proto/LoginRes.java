package unknow.sync.proto;

public class LoginRes {
	public unknow.sync.proto.pojo.UUID uuid;
	public unknow.sync.proto.pojo.ProjectInfo project;

	public LoginRes() {
	}

	public LoginRes(unknow.sync.proto.pojo.UUID uuid, unknow.sync.proto.pojo.ProjectInfo project) {
		this.uuid = uuid;
		this.project = project;
	}

	@Override
	public String toString() {
		return "LoginRes [uuid=" + uuid + ", project=" + project + "]";
	}
}
