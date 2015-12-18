package unknow.sync.mojo;

import java.util.regex.*;

import org.apache.maven.plugin.*;

public abstract class SyncMojo extends AbstractMojo
	{
	/**
	 * @parameter default-value="${project.build.directory}"
	 */
	protected String baseDir;

	/**
	 * @parameter
	 * @required
	 */
	protected String host;

	/**
	 * @parameter
	 * @required
	 */
	protected int port;

	/**
	 * @parameter
	 * @required
	 */
	protected String project;

	/**
	 * @parameter
	 */
	private String[] match;

	/**
	 * @parameter default-value="anonymous"
	 */
	protected String login;

	/**
	 * @parameter default-value=""
	 */
	protected String password;

	protected Pattern[] match()
		{
		if(match==null)
			return null;
		Pattern[] p=new Pattern[match.length];
		for(int i=0; i<p.length; i++)
			p[i]=Pattern.compile(match[i]);
		return p;
		}
	}
