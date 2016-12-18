package unknow.sync.server;

import unknow.common.cli.*;
import unknow.sync.server.cmd.*;

public class Shell extends AbstractShell
	{
	public Shell(SyncServ serv)
		{
		register(new InfoCmd(serv));
		register(new ProjectCmd(serv));
		}

	protected void prompt()
		{
		out().print("> ");
		out().flush();
		}

	protected void commandNotFound(String[] arg)
		{
		out().println("command '"+arg[0]+"' not found");
		}
	}
