package unknow.sync.server;

import java.util.HashSet;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class Cfg {
	@Option(name = "--port", aliases = "-p", required = true, usage = "Set the port to use")
	public int port;

	@Option(name = "--data", aliases = "-d", required = true, usage = "Set the root of all data")
	public String path;

	@Option(name = "--bloc-size", aliases = "-bs", required = true, usage = "Set the size of the bloc")
	public int blocSize;

	@Option(name = "--tokens", usage = "Set read only token")
	public Set<String> tokens = new HashSet<>();

	public static class SetOption extends OptionHandler<Set<String>> {
		/**
		 * create new SetOption
		 * 
		 * @param parser
		 * @param option
		 * @param setter
		 */
		public SetOption(CmdLineParser parser, OptionDef option, Setter<? super Set<String>> setter) {
			super(parser, option, setter);
		}

		@Override
		public int parseArguments(Parameters params) throws CmdLineException {
			String parameter = params.getParameter(0);
			String[] split = parameter.split(",");
			HashSet<String> set = new HashSet<>();
			for (String s : split)
				set.add(s);
			setter.addValue(set);
			return 1;
		}

		@Override
		public String getDefaultMetaVariable() {
			return "v1,v2,v..";
		}
	}
}
