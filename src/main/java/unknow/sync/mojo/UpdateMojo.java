package unknow.sync.mojo;

import org.apache.maven.plugin.*;

import unknow.sync.*;

/**
 * @goal update
 */
public class UpdateMojo extends SyncMojo
	{
	/**
	 * @parameter default-value=false
	 */
	private boolean delete;

	public void execute() throws MojoExecutionException, MojoFailureException
		{
		try
			{
			SyncClient sync=new SyncClient(host, port, baseDir);
			sync.setListener(new SyncListener.Log());
			sync.update(login, password, project, delete, match());
			sync.close();
			}
		catch (Throwable e)
			{
			throw new MojoExecutionException("", e);
			}
		}
	}
