package unknow.sync.mojo;

import org.apache.maven.plugin.*;

public abstract class SyncMojo extends AbstractMojo
	{
	/**
	 * @parameter
	 *         default-value="${project.build.directory}"
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
	protected String[] match;
	}
