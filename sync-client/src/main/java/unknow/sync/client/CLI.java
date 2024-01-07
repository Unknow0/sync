package unknow.sync.client;

import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * the command line interface
 * 
 * @author unknow
 */
public class CLI {
	private static final Option help = Option.builder("h").longOpt("help").desc("show this help").build();
	private static final Option token = Option.builder().longOpt("token").argName("token").hasArg().desc("the token to use (default to 'anonymous')").build();
	private static final Option path = Option.builder().longOpt("data").argName("folder").hasArg().desc("local directory to sync (default to current working directory)")
			.build();
	private static final Option temp = Option.builder().longOpt("temp").argName("folder").hasArg().desc("temporary folder").build();
	private static final Option regex = Option.builder().longOpt("includes").argName("regex").hasArg().desc("process only file that match the pattern").build();;

	/**
	 * the main
	 * 
	 * @param arg cli param
	 */
	public static void main(String[] arg) throws Exception {
		Options opts = new Options();
		opts.addOption(help);

		CommandLine cli = new DefaultParser().parse(opts, arg);
		if (cli.hasOption(help) || cli.getArgList().size() == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.setWidth(100);
			helpFormatter.printHelp("client <host:port>", opts);
			System.exit(2);
		}

		String host = "localhost";
		int port;
		String t = cli.getOptionValue(token, "anonymous");
		String tmp = cli.getOptionValue(temp, "temp");
		String p = cli.getOptionValue(path, "./");
		Pattern r = Pattern.compile(cli.getOptionValue(regex, ".*"));

		String a = cli.getArgList().get(0);
		int i = a.indexOf(':');
		if (i == 0)
			a = a.substring(1);
		else if (i > 0) {
			host = a.substring(0, i);
			a = a.substring(i + 1);
		}
		try {
			port = Integer.parseInt(a);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			System.err.println("invalid port number " + a);
			return;
		}

		new SyncRead(p, tmp, host, port).process(t, r);
	}
}
