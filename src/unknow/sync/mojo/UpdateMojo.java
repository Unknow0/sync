package unknow.sync.mojo;

import org.apache.maven.plugin.*;

import unknow.common.tools.*;
import unknow.sync.*;

/**
 * @goal update
 */
public class UpdateMojo extends SyncMojo
	{
	public void execute() throws MojoExecutionException, MojoFailureException
		{
		try
			{
			SyncClient sync=new SyncClient(host, port, baseDir);
			sync.login(login, password);
			sync.update(project, match==null?null:StringTools.collapse(match, "|"));
			sync.close();
			}
		catch (Exception e)
			{
			throw new MojoExecutionException("", e);
			}
		}
	}
