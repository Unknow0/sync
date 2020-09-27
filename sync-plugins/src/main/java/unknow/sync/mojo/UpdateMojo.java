package unknow.sync.mojo;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;

import unknow.sync.*;

@Mojo(name = "update")
public class UpdateMojo extends SyncMojo {
	@Parameter(property = "sync.delete", defaultValue = "false")
	private boolean delete;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			SyncClient sync = new SyncClient(host, port, baseDir);
			sync.setListener(new SyncListener.Log());
			sync.update(token, delete, pattern());
			sync.close();
		} catch (Throwable e) {
			throw new MojoExecutionException("", e);
		}
	}
}
