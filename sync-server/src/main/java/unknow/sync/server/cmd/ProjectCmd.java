//package unknow.sync.server.cmd;
//
//import java.io.*;
//import java.security.*;
//
//import unknow.common.cli.*;
//import unknow.json.*;
//import unknow.sync.server.*;
//
//public class ProjectCmd extends AbstractCmd {
//	private SyncServ serv;
//
//	public ProjectCmd(SyncServ serv) {
//		this.serv = serv;
//	}
//
//	public void reload(Shell shell, String[] arg) throws NoSuchAlgorithmException, IOException, JsonException {
//		if (arg.length == 1) {
//			shell.out().println("Reloading ...");
//			serv.reload();
//			shell.out().println("Done.");
//		}
//	}
//}
