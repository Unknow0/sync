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

	protected Pattern pattern()
		{
		if(match==null||match.length==0)
			return null;
		if(match.length==1)
			return Pattern.compile(match[0]);

		StringBuilder sb=new StringBuilder();
		sb.append("(?:").append(match[0]).append(")");
		for(int i=1; i<match.length; i++)
			sb.append("|(?:").append(match[i]).append(")");
		return Pattern.compile(sb.toString());
		}
	}
