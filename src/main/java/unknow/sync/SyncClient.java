package unknow.sync;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import org.apache.avro.*;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.logging.log4j.*;

import unknow.common.cli.*;
import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.*;
import unknow.sync.proto.UUID;

/**
 * 
 * @author Unknow
 */
public class SyncClient
	{
	private static final Logger log=LogManager.getFormatterLogger(SyncClient.class);
	protected Path path;

	protected int blocSize;

	private Sync sync;
	private NettyTransceiver client;

	protected SyncListener listener;
	protected UUID uuid;

	public SyncClient(String host, int port, String p) throws UnknownHostException, IOException
		{
		path=Paths.get(p);
		client=new NettyTransceiver(new InetSocketAddress(host, port));
		sync=SpecificRequestor.getClient(Sync.class, client);
		}

	public void update(String login, String pass, String project, boolean delete, Pattern pattern) throws IOException, InterruptedException, NoSuchAlgorithmException
		{
		LoginRes res=sync.login(login, pass, project, Action.read);
		uuid=res.getUuid();
		ProjectInfo info=res.getProject();

		blocSize=info.getBlocSize();

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize, pattern);

		Matcher m=pattern==null?null:pattern.matcher("");

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.getHashs().size()/.75));
		for(int i=0; i<info.getHashs().size(); i++)
			{
			FileHash h=info.getHashs().get(i);
			if(m!=null)
				m.reset(h.getName());
			if(m==null||m.matches())
				map.put(h.getName(), new IndexedHash(i, h.getHash()));
			}

		// remove unmodified files
		Iterator<FileDesc> it=files.iterator();
		List<Integer> list=new ArrayList<Integer>(files.size());
		List<String> filetoDelete=new ArrayList<String>();
		while (it.hasNext())
			{
			FileDesc fd=it.next();
			IndexedHash p=map.remove(fd.getName());
			if(p!=null&&p.h.equals(fd.getFileHash()))
				it.remove();
			else if(p==null)
				{
				filetoDelete.add(fd.getName());
				it.remove();
				}
			else if(p!=null)
				list.add(p.i);
			}

		if(listener!=null)
			listener.startUpdate(project, files.size(), map.size(), filetoDelete.size());

		if(delete)
			{
			for(String s:filetoDelete)
				Files.delete(path.resolve(s));
			}

		// get modified file desc
		List<FileDesc> l=sync.getFileDescs(uuid, list);
		for(FileDesc server:l)
			{
			it=files.iterator();
			while (it.hasNext())
				{
				FileDesc local=it.next();
				if(local.getName().equals(server.getName()))
					{
					UpdateProcessor.update(this, local, server);

					it.remove();
					break;
					}
				}
			}
		for(Map.Entry<String,IndexedHash> e:map.entrySet())
			{
			if(listener!=null)
				listener.startFile(e.getKey());
			File f=new File(path.toFile(), e.getKey());
			boolean done=false;
			do
				{
				if(listener!=null)
					listener.startReconstruct(e.getKey());
				done=getFile(f, e.getKey(), e.getValue().h);
				if(listener!=null)
					listener.doneReconstruct(e.getKey(), f.length(), done);
				}
			while (!done);

			if(listener!=null)
				listener.doneFile(e.getKey(), f.length());
			}

		if(listener!=null)
			listener.doneUpdate(project);
		}

	public void commit(String login, String pass, String project, Pattern pattern) throws NoSuchAlgorithmException, IOException, InterruptedException
		{
		LoginRes res=sync.login(login, pass, project, Action.write);
		uuid=res.getUuid();
		ProjectInfo info=res.getProject();

		blocSize=info.getBlocSize();

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize, pattern);

		Matcher m=pattern==null?null:pattern.matcher("");

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.getHashs().size()/.75));
		for(int i=0; i<info.getHashs().size(); i++)
			{
			FileHash h=info.getHashs().get(i);
			if(m!=null)
				m.reset(h.getName());
			if(m==null||m.matches())
				map.put(h.getName(), new IndexedHash(i, h.getHash()));
			}

		// remove unmodified files
		Iterator<FileDesc> it=files.iterator();
		List<Integer> list=new ArrayList<Integer>(files.size());
		List<FileDesc> fileToCreate=new ArrayList<FileDesc>();
		while (it.hasNext())
			{
			FileDesc fd=it.next();
			IndexedHash p=map.remove(fd.getName());
			if(p!=null&&p.h.equals(fd.getFileHash()))
				it.remove();
			else if(p==null)
				{
				fileToCreate.add(fd);
				it.remove();
				}
			else if(p!=null)
				list.add(p.i);
			}

		if(listener!=null)
			listener.startUpdate(project, files.size(), fileToCreate.size(), map.size());

		// get modified file desc
		List<FileDesc> l=sync.getFileDescs(uuid, list);
		for(FileDesc server:l)
			{
			it=files.iterator();
			while (it.hasNext())
				{
				FileDesc local=it.next();
				if(local.getName().equals(server.getName()))
					{
					if(listener!=null)
						listener.startFile(local.getName());
					CommitProcessor.commit(this, local, server);
					if(listener!=null)
						listener.doneFile(local.getName(), 0);
					it.remove();
					break;
					}
				}
			}

		for(FileDesc local:fileToCreate)
			{
			if(listener!=null)
				listener.startFile(local.getName());
			CommitProcessor.send(this, local);
			if(listener!=null)
				listener.doneFile(local.getName(), 0);
			}

		for(Map.Entry<String,IndexedHash> e:map.entrySet())
			sync.delete(uuid, e.getKey());
		
		if(listener!=null)
			listener.doneUpdate(project);
		}

	public void close()
		{
		client.close();
		}

	public boolean getFile(File file, String name, Hash h) throws IOException, NoSuchAlgorithmException
		{
		MessageDigest md=MessageDigest.getInstance("SHA-512");
		try (FileOutputStream fos=new FileOutputStream(file))
			{
			byte[] buf=new byte[0];
			long off=0;
			while (true)
				{
				ByteBuffer bb=sync.getFile(uuid, name, off);
				if(bb==null) // done
					break;
				int len=bb.remaining();
				if(buf.length<len)
					buf=new byte[len];
				bb.get(buf, 0, len);
				fos.write(buf, 0, len);
				md.update(buf, 0, len);
				off+=len;
				}
			}
		byte[] digest=md.digest();
		return Arrays.equals(digest, h.bytes());
		}

	public ByteBuffer getBloc(String name, int i, int len) throws SyncException, AvroRemoteException
		{
		return sync.getBloc(uuid, name, i, len);
		}

	public void setListener(SyncListener listener)
		{
		this.listener=listener;
		}

	public static void main(String arg[]) throws Throwable
		{
		String host=null;
		int port=54323;
		String path="./";
		String login=null;
		String pass=null;
		Args args=new Args(arg);
		String a;
		while ((a=args.next())!=null)
			{
			if(a.equals("host")||a.equals("H"))
				host=args.nextValue();
			else if(a.equals("port")||a.equals("P"))
				port=Integer.parseInt(args.nextValue());
			else if(a.equals("login")||a.equals("l"))
				login=args.nextValue();
			else if(a.equals("pass")||a.equals("p"))
				pass=args.nextValue();
			else if(a.equals("path")||a.equals("d"))
				path=args.nextValue();
			else if(a.equals("help"))
				{
				log.info("usage: <option> <cmd> <project>");
				log.info("Option");
				log.info("	-H <host> |--host=<host>");
				log.info("		Set the server host to use (required).");
				log.info("	-P <port> | --port=<port>");
				log.info("		Set the port to use, use the default port if not specified");
				log.info("	-l <login> | --login=<login>");
				log.info("		Set login.");
				log.info("	-p <pass> | --pass=<pass>");
				log.info("	-d <path> | --path=<path>");
				log.info("	--help");
				log.info("		This help.");
				log.info("Command");
				log.info("	update");
				log.info("	commit");
				return;
				}
			}
		if(host==null)
			{
			log.error("host required");
			return;
			}
		SyncClient cl=new SyncClient(host, port, path);
		cl.setListener(new SyncListener.Log());
		String str=args.left();

		try
			{
			if(str.equalsIgnoreCase("update"))
				cl.update(login, pass, args.left(), false, null);
			else if(str.equalsIgnoreCase("commit"))
				cl.commit(login, pass, args.left(), null);
			else
				log.error("invalid command");
			}
		catch (SyncException e)
			{
			log.error(e.getDesc());
			}
		finally
			{
			cl.close();
			}
		}

	public void startAppend(String name) throws AvroRemoteException
		{
		sync.startAppend(uuid, name);
		}

	public void appendData(ByteBuffer bbuf) throws AvroRemoteException
		{
		sync.appendData(uuid, bbuf);
		}

	public void appendBloc(Integer bloc, int count) throws AvroRemoteException
		{
		sync.appendBloc(uuid, bloc, count);
		}

	public FileDesc endAppend(Hash expectedHash) throws AvroRemoteException
		{
		return sync.endAppend(uuid, expectedHash);
		}
	}
