package unknow.sync.client;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * the command line interface
 * 
 * @author unknow
 */
public class CLI implements Callable<Integer> {

	/** the server's host */
	@Option(names = "--host", required = true, description = "Set the server host to use")
	public String host;

	/** the server's port */
	@Option(names = "--port", required = true, description = "Set the port to use")
	public int port;

	/** the token to use */
	@Option(names = "--token", description = "the token to use (default to 'anonymous')")
	public String token = "anonymous";

	/** the data path */
	@Option(names = "--data", description = "local directory to sync (default to current working directory)")
	public String path = "./";

	/** the temporary folder */
	@Option(names = "--temp", description = "temporary folder")
	public String temp = "temp";

	/** a regexp to limit processing */
	@Option(names = "--regex", description = "process only file that match the pattern")
	public Pattern regex;

	@Override
	public Integer call() throws Exception {
		new SyncRead(path, temp, host, port).process(token, regex);
		return 0;
	}

	/**
	 * the main
	 * 
	 * @param arg cli param
	 */
	public static void main(String[] arg) {
		System.exit(new CommandLine(new CLI()).execute(arg));
	}
}
