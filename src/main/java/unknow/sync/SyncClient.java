package unknow.sync;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import org.slf4j.*;

import unknow.common.cli.*;
import unknow.common.tools.*;
import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.*;
import unknow.sync.proto.pojo.*;
import unknow.sync.proto.pojo.UUID;

import com.esotericsoftware.kryo.io.*;

/**
 * 
 * @author Unknow
 */
public class SyncClient implements AutoCloseable
	{
	private static final Logger log=LoggerFactory.getLogger(SyncClient.class);
	protected Path path;

	protected int blocSize;

	private Kryos kryo;
	private Input in;
	private Output out;
	private Socket socket;

	protected SyncListener listener;
	protected UUID uuid;

	public SyncClient(String host, int port, String p) throws UnknownHostException, IOException, NoSuchAlgorithmException
		{
		path=Paths.get(p);
		socket=new Socket(host, port);
		in=new Input(socket.getInputStream());
		out=new Output(socket.getOutputStream());
		kryo=new Kryos();
		}

	@SuppressWarnings("unchecked")
	private <T> T send(Object o) throws SyncException
		{
		kryo.write(out, o);
		Object r=kryo.read(in);
		if(r instanceof String)
			throw new SyncException((String)r);
		return (T)r;
		}

	public void update(String login, String pass, String project, boolean delete, Pattern pattern) throws IOException, InterruptedException, NoSuchAlgorithmException, SyncException
		{
		LoginRes res=send(new LoginReq(login, pass, project, Action.read));
		uuid=res.uuid;
		ProjectInfo info=res.project;

		blocSize=info.blocSize;

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize, pattern);

		Matcher m=pattern==null?null:pattern.matcher("");

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.hashs.length/.75));
		for(int i=0; i<info.hashs.length; i++)
			{
			FileHash h=info.hashs[i];
			if(m!=null)
				m.reset(h.name);
			if(m==null||m.matches())
				map.put(h.name, new IndexedHash(i, h.hash));
			}

		// remove unmodified files
		Iterator<FileDesc> it=files.iterator();
		int[] list=new int[files.size()];
		List<String> filetoDelete=new ArrayList<String>();
		int k=0;
		while (it.hasNext())
			{
			FileDesc fd=it.next();
			IndexedHash p=map.remove(fd.name);
			if(p!=null&&p.h.equals(fd.fileHash))
				it.remove();
			else if(p==null)
				{
				filetoDelete.add(fd.name);
				it.remove();
				}
			else if(p!=null)
				list[k++]=p.i;
			}

		if(listener!=null)
			listener.startUpdate(project, files.size(), map.size(), filetoDelete.size());

		if(delete)
			{
			for(String s:filetoDelete)
				Files.delete(path.resolve(s));
			}

		// get modified file desc
		FileDesc[] l=send(new GetFileDescs(uuid, list));
		for(FileDesc server:l)
			{
			it=files.iterator();
			while (it.hasNext())
				{
				FileDesc local=it.next();
				if(local.name.equals(server.name))
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

	public void commit(String login, String pass, String project, Pattern pattern) throws NoSuchAlgorithmException, IOException, InterruptedException, SyncException
		{
		LoginRes res=send(new LoginReq(login, pass, project, Action.write));
		uuid=res.uuid;
		ProjectInfo info=res.project;

		blocSize=info.blocSize;

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize, pattern);

		Matcher m=pattern==null?null:pattern.matcher("");

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.hashs.length/.75));
		for(int i=0; i<info.hashs.length; i++)
			{
			FileHash h=info.hashs[i];
			if(m!=null)
				m.reset(h.name);
			if(m==null||m.matches())
				map.put(h.name, new IndexedHash(i, h.hash));
			}

		// remove unmodified files
		Iterator<FileDesc> it=files.iterator();
		List<Integer> list=new ArrayList<Integer>(files.size());
		List<FileDesc> fileToCreate=new ArrayList<FileDesc>();
		while (it.hasNext())
			{
			FileDesc fd=it.next();
			IndexedHash p=map.remove(fd.name);
			if(p!=null&&p.h.equals(fd.fileHash))
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
		FileDesc[] l=send(new GetFileDescs(uuid, CollectionUtils.toIntArray(list)));
		for(FileDesc server:l)
			{
			it=files.iterator();
			while (it.hasNext())
				{
				FileDesc local=it.next();
				if(local.name.equals(server.name))
					{
					if(listener!=null)
						listener.startFile(local.name);
					long fileSize=CommitProcessor.commit(this, local, server);
					if(listener!=null)
						listener.doneFile(local.name, fileSize);
					it.remove();
					break;
					}
				}
			}

		for(FileDesc local:fileToCreate)
			{
			if(listener!=null)
				listener.startFile(local.name);
			long fileSize=CommitProcessor.send(this, local);
			if(listener!=null)
				listener.doneFile(local.name, fileSize);
			}

		for(Map.Entry<String,IndexedHash> e:map.entrySet())
			send(new DeleteReq(uuid, e.getKey()));

		if(listener!=null)
			listener.doneUpdate(project);
		}

	public void close() throws IOException
		{
		in.close();
		out.close();
		socket.close();
		}

	public boolean getFile(File file, String name, Hash h) throws IOException, NoSuchAlgorithmException, SyncException
		{
		MessageDigest md=MessageDigest.getInstance("SHA-512");
		file.getParentFile().mkdirs();
		try (FileOutputStream fos=new FileOutputStream(file))
			{
			long off=0;
			while (true)
				{
				Object o=send(new GetFileReq(uuid, name, off));
				if(o instanceof Done) // done
					break;
				byte[] buf=(byte[])o;
				fos.write(buf);
				md.update(buf);
				off+=buf.length;
				}
			}
		byte[] digest=md.digest();
		return Arrays.equals(digest, h.bytes);
		}

	public byte[] getBloc(String name, int i, int len) throws SyncException
		{
		return send(new GetBlocReq(uuid, name, i, len));
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
		String login="anonymous";
		String pass="";
		Pattern filter=null;
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
			else if(a.equals("filter")||a.equals("f"))
				filter=Pattern.compile(args.nextValue());
			else if(a.equals("help"))
				{
				log.info("usage: <option> <cmd> <project>");
				log.info("Option");
				log.info("	-H <host> |--host=<host>");
				log.info("		Set the server host to use (required).");
				log.info("	-P <port> | --port=<port>");
				log.info("		Set the port to use (default to 54323)");
				log.info("	-l <login> | --login=<login>");
				log.info("		Set login (default to 'anonymous')");
				log.info("	-p <pass> | --pass=<pass>");
				log.info("		set the password to use (default to empty)");
				log.info("	-f <regexp> | --filter=<regexp>");
				log.info("		process only file that match the regexp");
				log.info("	-d <path> | --path=<path>");
				log.info("		the path to the local data (default to current directory)");
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
				cl.update(login, pass, args.left(), false, filter);
			else if(str.equalsIgnoreCase("commit"))
				cl.commit(login, pass, args.left(), filter);
			else
				log.error("invalid command");
			}
		catch (SyncException e)
			{
			log.error(e.getMessage());
			}
		finally
			{
			try
				{
				cl.close();
				}
			catch (Exception e)
				{
				log.error("failed to close", e);
				}
			}
		}

	public void startAppend(String name) throws SyncException
		{
		kryo.write(out, new StartAppend(uuid, name));
		}

	public void appendData(byte[] bbuf) throws SyncException
		{
		kryo.write(out, new AppendData(uuid, bbuf));
		}

	public void appendBloc(Integer bloc, int count) throws SyncException
		{
		kryo.write(out, new AppendBloc(uuid, bloc, count));
		}

	public FileDesc endAppend(Hash expectedHash) throws SyncException
		{
		Object o=send(new EndAppend(uuid, expectedHash));
		return o instanceof FileDesc?(FileDesc)o:null;
		}
	}
