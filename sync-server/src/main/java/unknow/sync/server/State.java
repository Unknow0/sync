package unknow.sync.server;

import java.io.*;

import unknow.sync.proto.pojo.*;

public class State {
	Project project = null;
	String appendFile;
	File tmp;
	RandomAccessFile orgFile = null;
	FileOutputStream fos = null;
	UUID uuid;
	Action action;
}