package unknow.sync;

import java.util.regex.Pattern;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the command line interface
 * 
 * @author unknow
 */
public class Cli {
	private static final Logger log = LoggerFactory.getLogger(Cli.class);

	private static class Cfg {
		@Option(name = "--host", required = true, usage = "Set the server host to use")
		public String host;

		@Option(name = "--port", required = true, usage = "Set the port to use")
		public int port;

		@Option(name = "--user", usage = "the user (default to 'anonymous')")
		public String user = "anonymous";

		@Option(name = "--pass", usage = "the password")
		public String pass = "";

		@Option(name = "-d", usage = "local directory to sync (default to current working directory)")
		public String path = "./";

		@Option(name = "--regexp", usage = "process only file that match the regexp")
		public Pattern regexp;

		@Argument(usage = "the action (update/commit)")
		public String action;

		@Argument(index = 1, usage = "the project to operate on")
		public String project;
	}

	/**
	 * @param arg
	 * @throws Throwable
	 */
	public static void main(String arg[]) throws Throwable {
		Cfg cfg = new Cfg();

		CmdLineParser cmdLineParser = new CmdLineParser(cfg);
		try {
			cmdLineParser.parseArgument(arg);
		} catch (CmdLineException e) {
			cmdLineParser.printUsage(System.out);
			System.exit(1);
		}
		SyncClient cl = new SyncClient(cfg.host, cfg.port, cfg.path);
		cl.setListener(new SyncListener.Log());
		String str = cfg.action;

		try {
			if (str.equalsIgnoreCase("update"))
				cl.update(cfg.user, cfg.pass, cfg.project, false, cfg.regexp);
			else if (str.equalsIgnoreCase("commit"))
				cl.commit(cfg.user, cfg.pass, cfg.project, cfg.regexp);
			else
				log.error("invalid command");
		} catch (SyncException e) {
			log.error(e.getMessage());
		} finally {
			try {
				cl.close();
			} catch (Exception e) {
				log.error("failed to close", e);
			}
		}
	}
}
