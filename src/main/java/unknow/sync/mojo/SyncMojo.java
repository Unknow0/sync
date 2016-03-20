package unknow.sync.mojo;

import java.util.*;
import java.util.regex.*;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;

public abstract class SyncMojo extends AbstractMojo
	{
	@Parameter(property="sync.baseDir", defaultValue="${project.build.directory}")
	protected String baseDir;

	@Parameter(property="sync.host", required=true)
	protected String host;

	@Parameter(property="sync.port", required=true)
	protected int port;

	@Parameter(property="sync.project", required=true)
	protected String project;

	@Parameter(property="sync.match")
	private String[] match;

	@Parameter(property="sync.login", defaultValue="anonymous")
	protected String login;

	@Parameter(property="sync.password", defaultValue="")
	protected String password;

	protected Pattern pattern()
		{
		getLog().error(Arrays.toString(match));
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
