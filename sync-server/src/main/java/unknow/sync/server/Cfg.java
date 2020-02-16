package unknow.sync.server;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import unknow.sync.proto.pojo.Action;

public class Cfg {
	public int port;
	public Map<String, P> projects;
	public Map<String, String> users;

	public static class P {
		public String path;
		public int bloc_size;

		public EnumMap<Action, Set<String>> rights;
	}
}
