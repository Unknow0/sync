package unknow.sync.mojo;

import org.apache.maven.plugin.*;

import unknow.sync.*;

/**
 * @goal commit
 */
public class CommitMojo extends SyncMojo
	{
	public void execute() throws MojoExecutionException, MojoFailureException
		{
		try
			{
			SyncClient sync=new SyncClient(host, port, baseDir);
			sync.setListener(new SyncListener.Log());
			sync.commit(login, password, project, match());
			sync.close();
			}
		catch (Exception e)
			{
			throw new MojoExecutionException("", e);
			}
		}
	}
