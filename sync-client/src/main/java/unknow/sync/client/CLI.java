package unknow.sync.client;

import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import unknow.log.Log;
import unknow.log.LogFactory;

/**
 * the command line interface
 * 
 * @author unknow
 */
public class CLI {
	private static final Log log = LogFactory.getLogger(CLI.class);

	/** the server's host */
	@Option(name = "--host", required = true, usage = "Set the server host to use")
	public String host;

	/** the server's port */
	@Option(name = "--port", required = true, usage = "Set the port to use")
	public int port;

	/** the token to use */
	@Option(name = "--token", usage = "the token to use (default to 'anonymous')")
	public String token = "anonymous";

	/** the data path */
	@Option(name = "--data", usage = "local directory to sync (default to current working directory)")
	public String path = "./";

	/** the temporary folder */
	@Option(name = "--temp", usage = "temporary folder")
	public String temp = "temp";

	/** a regexp to limit processing */
	@Option(name = "--regex", usage = "process only file that match the pattern")
	public Pattern regex;

	/**
	 * @param arg
	 * @throws Throwable
	 */
	public static void main(String arg[]) throws Throwable {
		CLI cfg = new CLI();
		CmdLineParser cmdLineParser = new CmdLineParser(cfg);
		try {
			cmdLineParser.parseArgument(arg);
		} catch (CmdLineException e) {
			cmdLineParser.printUsage(System.out);
			System.exit(1);
		}
		try {
			new SyncRead(cfg.path, cfg.temp, cfg.host, cfg.port).process(cfg.token, cfg.regex);
		} catch (Exception e) {
			log.error("", e);
		} finally {
			try {
			} catch (Exception e) {
				log.error("failed to close", e);
			}
		}
	}
}
