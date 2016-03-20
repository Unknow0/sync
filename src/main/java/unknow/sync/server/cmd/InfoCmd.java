package unknow.sync.server.cmd;

import java.util.*;

import unknow.common.cli.*;
import unknow.sync.proto.pojo.*;
import unknow.sync.server.*;

public class InfoCmd extends AbstractCmd
	{
	private SyncServ serv;

	public InfoCmd(SyncServ serv)
		{
		this.serv=serv;
		}

	public void info(Shell shell, String[] arg)
		{
		if(arg.length==1)
			{
			Map<String,Project> projects=serv.projects();
			for(Map.Entry<String,Project> e:projects.entrySet())
				shell.out().format("%-40s: %d files\n", e.getKey(), e.getValue().fileDesc().size());
			}
		else if(arg.length==2)
			{
			Project p=serv.getProject(arg[1]);
			shell.out().println(p.path());
			for(FileDesc fd:p.fileDesc())
				shell.out().format("%-80s: %d blocs\n", fd.name, fd.blocs.length);
			}
		}
	}
