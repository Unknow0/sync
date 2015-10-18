package unknow.sync;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.logging.log4j.*;

import unknow.common.*;
import unknow.common.cli.*;
import unknow.common.tools.*;
import unknow.sync.proto.*;
import unknow.sync.proto.UUID;

/**
 * 
 * @author Unknow
 */
public class SyncClient
	{
	private static final Logger log=LogManager.getFormatterLogger(SyncClient.class);
	private String path;

	private int blocSize;

	private Sync sync;
	private NettyTransceiver client;

	private UUID uuid;

	private UpdateListener listener;

	public SyncClient(String host, int port, String p) throws UnknownHostException, IOException
		{
		path=p;
		client=new NettyTransceiver(new InetSocketAddress(host, port));
		sync=SpecificRequestor.getClient(Sync.class, client);
		}

	public boolean login(String login, String pass) throws IOException
		{
		UUID u=sync.login(login, pass);
		if(u!=null)
			uuid=u;
		return u!=null;
		}

	private JoinableThreadGroup threadGroup=new JoinableThreadGroup("sync-worker");

	public int update(String project) throws IOException, InterruptedException
		{
		return update(project, null);
		}

	public int update(String project, String regexp) throws IOException, InterruptedException
		{
		ProjectInfo info=sync.selectProject(uuid, project);
		blocSize=info.getBlocSize();
		log.debug("bloc size: "+blocSize);
		log.debug("file count: "+info.getFileDescs().size());

		if(listener!=null)
			listener.startUpdate(project, info.getFileDescs().size());

		Matcher m=null;
		if(regexp!=null)
			{
			m=Pattern.compile(regexp).matcher("");
			}
		for(FileDesc f:info.getFileDescs())
			{
			if(m!=null)
				{
				m.reset(f.getName());
				if(!m.matches())
					continue;
				}
			UpdateDesc updateDesc=new UpdateDesc(f);
			updateDesc.start();
			}
		threadGroup.join();
		if(listener!=null)
			listener.doneUpdate(project);
		return 0;
		}

	public int commit(String project) throws NoSuchAlgorithmException, IOException, InterruptedException
		{
		return commit(project, null);
		}

	public int commit(String project, String regexp) throws NoSuchAlgorithmException, IOException, InterruptedException
		{
		ProjectInfo info=sync.selectProject(uuid, project);
		blocSize=info.getBlocSize();

		Set<FileDesc> set=new HashSet<FileDesc>();
		Common.load(set, path, "", blocSize);

		Matcher m=Pattern.compile(regexp).matcher("");

		List<FileDesc> fileDescs=info.getFileDescs();
		log.debug("file count: "+fileDescs.size()+" / "+set.size());
		l1: for(FileDesc desc:fileDescs)
			{
			if(m!=null)
				{
				m.reset(desc.getName());
				if(!m.matches())
					continue;
				}
			Iterator<FileDesc> it=set.iterator();
			while (it.hasNext())
				{
				FileDesc fd=it.next();
				if(fd.getName().toString().contentEquals(desc.getName()))
					{
					CommitDesc commitDesc=new CommitDesc(desc, fd.getFileHash());
					commitDesc.start();
					it.remove();
					continue l1;
					}
				}
			log.debug("delete '"+desc.getName()+"'");
			synchronized (sync)
				{
				sync.delete(uuid, desc.getName());
				}
			}
		for(FileDesc fd:set)
			{
			if(m!=null)
				{
				m.reset(fd.getName());
				if(!m.matches())
					continue;
				}
			synchronized (sync)
				{
				File f=new File(path, fd.getName().toString());
				FileInputStream fis=new FileInputStream(f);
				log.debug("adding new file "+fd.getName());
				sync.startAppend(uuid, fd.getName());

				byte[] b=new byte[2048];
				ByteBuffer bbuf=ByteBuffer.wrap(b);

				int r;
				while ((r=fis.read(b))>0)
					{
					bbuf.limit(r);
					sync.appendData(uuid, bbuf);
					}
				sync.endAppend(uuid, fd.getFileHash()); // TODO check values

				fis.close();
				}
			}
		threadGroup.join();
		return 0;
		}

	public void close()
		{
		client.close();
		}

	public class UpdateDesc extends Thread
		{
		private FileDesc fd;
		private Map<Integer,Long> blocFound=new HashMap<Integer,Long>();
		private File file;

		public UpdateDesc(FileDesc f)
			{
			super(threadGroup, "Update "+f.getName());
			fd=f;
			file=new File(path, fd.getName().toString());
			}

		public void run()
			{
			Map<Integer,P> hash=new HashMap<Integer,P>();
			for(int i=0; i<fd.getBlocCount(); i++)
				hash.put(fd.getRoll().get(i), new P(i, fd.getHash().get(i)));

			try
				{
				boolean done;
				do
					{
					blocFound.clear();
					long start=System.currentTimeMillis();
					found(hash);

					if(listener!=null)
						listener.startCheckFile(fd.getName(), fd.getBlocCount());
					log.debug("computing hash %s took %d ms", fd.getName(), System.currentTimeMillis()-start);
					if(listener!=null)
						listener.doneCheckFile(fd.getName(), fd.getBlocCount()-blocFound.size());
					synchronized (sync)
						{
						done=reconstruct();
						}
					long d=System.currentTimeMillis()-start;
					if(listener!=null)
						listener.doneReconstruct(fd.getName(), done);
					log.info("updating %s took %d ms (%.3f Ko/sec)", fd.getName(), d, 1000.*file.length()/(8.*d));
					}
				while (!done);

				if(listener!=null)
					listener.doneFile(fd.getName());
				}
			catch (NoSuchAlgorithmException e)
				{
				e.printStackTrace();
				}
			catch (IOException e)
				{
				e.printStackTrace();
				}
			}

		public void found(Map<Integer,P> hash) throws IOException, NoSuchAlgorithmException
			{
			try
				{
				log.info("start check "+fd.getName());
				FileInputStream fis=new FileInputStream(file);
				RollingChecksum rcs=new RollingChecksum(blocSize);

				MessageDigest md=MessageDigest.getInstance("SHA-512");

				long len=file.length();

				/** read first block */
				int c;
				for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
					rcs.append((byte)c);
				long off=0;
				while ((c=fis.read())!=-1)
					{
					int r=rcs.append((byte)c);
					P p=hash.get(r);
					if(p!=null)
						{ // found match
						if(Arrays.equals(md.digest(rcs.buf()), p.hash.bytes()))
							blocFound.put(p.i, off);
						md.reset();
						}
					off++;
					if(listener==null&&off%blocSize==0)
						listener.updateCheck(fd.getName(), 1f*off/len);
					}
				// test with padding
				for(int i=0; i<blocSize-1; i++)
					{
					int r=rcs.append((byte)0);
					if(r==fd.getRoll().get(fd.getRoll().size()-1))
						{ // found match
						if(Arrays.equals(md.digest(rcs.buf()), fd.getHash().get(fd.getRoll().size()-1).bytes()))
							blocFound.put(fd.getRoll().size()-1, off);
						md.reset();
						}
					off++;
					}

				fis.close();
				}
			catch (FileNotFoundException e)
				{
				}
			}

		public boolean reconstruct() throws IOException, NoSuchAlgorithmException
			{
			if(fd.getBlocCount()==blocFound.size())
				return true; // file unchanged
			log.debug("start reconstruct");

			File tmp=File.createTempFile("sync", file.getName());
			try (FileOutputStream fos=new FileOutputStream(tmp))
				{
				MessageDigest md=MessageDigest.getInstance("SHA-512");

				if(blocFound.size()<fd.getBlocCount()*.6)
					{ // download full file
					log.debug("'%s' new", fd.getName());
					Long len=sync.getFile(uuid, fd.getName());
					long off=0;
					if(len==null)
						{
						log.debug("failed to get file");
						return false;
						}
					byte buf[]=new byte[2048];
					ByteBuffer data=sync.getNext(uuid);
					while (data!=null)
						{
						off+=data.limit();
						if(data.limit()>buf.length)
							buf=new byte[data.limit()];
						data.get(buf, 0, data.limit());
						fos.write(buf, 0, data.limit());
						md.update(buf, 0, data.limit());

						if(listener!=null)
							listener.updateReconstruct(fd.getName(), 1f*off/len);

						data=sync.getNext(uuid);
						}
					}
				else
					{
					try (RandomAccessFile ram=new RandomAccessFile(file, "r"))
						{
						byte[] buf=new byte[blocSize];
						for(int i=0; i<fd.getBlocCount(); i++)
							{
							if(blocFound.containsKey(i))
								{
								log.debug("read bloc "+i+" from "+fd.getName());
								log.debug(blocFound.get(i));
								ram.seek(blocFound.get(i));
								int bs=buf.length;
								int s;
								while (bs>0&&(s=ram.read(buf, 0, bs))>0)
									{
									fos.write(buf, 0, s);
									md.update(buf, 0, s);
									bs-=s;
									}
								}
							else
								{ // get it from serv
								log.debug("read bloc "+i+" from serv");
								ByteBuffer bloc=sync.getBloc(uuid, fd.getName(), i);
								if(bloc==null)
									{
									log.debug("failed to get bloc");
									return false;
									}
								fos.write(bloc.array());
								md.update(bloc.array());
								}
							}
						ram.close();
						}
					}
				fos.close();
				file.getParentFile().mkdirs();
				Files.move(Paths.get(tmp.getAbsolutePath()), Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
				byte[] digest=md.digest();
				log.debug("got: "+StringTools.toHex(digest));
				log.debug("exp: "+StringTools.toHex(fd.getFileHash().bytes()));
				return Arrays.equals(digest, fd.getFileHash().bytes());
				}
			}
		}

	private static class P
		{
		int i;
		Hash hash;

		public P(int i, Hash hash)
			{
			this.i=i;
			this.hash=hash;
			}
		}

	private class CommitDesc extends Thread
		{
		private FileDesc fd;
		private Hash expectedHash;
		private File file;

		private Map<Long,Integer> blocOff=new TreeMap<Long,Integer>();
		private boolean delete=false;

		public CommitDesc(FileDesc f, Hash hash)
			{
			super(threadGroup, "Commit "+f.getName());
			fd=f;
			expectedHash=hash;
			file=new File(path, fd.getName().toString());
			}

		public void run()
			{
			Map<Integer,P> hash=new HashMap<Integer,P>();
			for(int i=0; i<fd.getBlocCount(); i++)
				hash.put(fd.getRoll().get(i), new P(i, fd.getHash().get(i)));

			try
				{
				boolean done;
				do
					{
					long start=System.currentTimeMillis();
					found(hash);
					log.debug("computing hash %s took %d ms", fd.getName(), System.currentTimeMillis()-start);
					synchronized (sync)
						{
						done=sendReconstruct();
						}
					long d=System.currentTimeMillis()-start;
					log.info("updating %s took %d ms (%.3f Ko/sec)", fd.getName(), d, 1000.*file.length()/(8.*d));
					log.debug("done: "+done);
					}
				while (!done);
				}
			catch (NoSuchAlgorithmException e)
				{
				e.printStackTrace();
				}
			catch (IOException e)
				{
				e.printStackTrace();
				}
			}

		private void found(Map<Integer,P> hash) throws IOException, NoSuchAlgorithmException
			{
			try
				{
				log.debug("start check "+fd.getName());
				FileInputStream fis=new FileInputStream(file);
				RollingChecksum rcs=new RollingChecksum(blocSize);

				MessageDigest md=MessageDigest.getInstance("SHA-512");

				/** read first block */
				int c;
				for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
					rcs.append((byte)c);
				long off=0;
				loop: while ((c=fis.read())!=-1)
					{
					int r=rcs.append((byte)c);

					P p=hash.get(r);
					if(p!=null)
						{ // found match
						Hash h=new Hash(md.digest(rcs.buf()));
						md.reset();
						if(h.equals(p.hash))
							{
							blocOff.put(off, p.i);

							for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
								rcs.append((byte)c);
							off+=blocSize;
							continue loop;
							}
						}
					off++;
					}
				// test with padding
				int lastRoll=fd.getRoll().get(fd.getRoll().size()-1);
				Hash lastHash=fd.getHash().get(fd.getRoll().size()-1);
				for(int i=0; i<blocSize; i++)
					{
					int r=rcs.append((byte)0);
					if(r==lastRoll)
						{ // found match
						Hash h=new Hash(md.digest(rcs.buf()));
						if(h.equals(lastHash))
							{
							blocOff.put(off, fd.getRoll().size()-1);
							break;
							}
						md.reset();
						}
					off++;
					}
				log.debug(fd.getName()+"	bloc found: "+blocOff.size()+"/"+fd.getBlocCount());
				fis.close();
				}
			catch (FileNotFoundException e)
				{
				delete=true;
				}
			}

		public boolean sendReconstruct() throws IOException
			{
			log.debug("start reconstruct "+fd.getName()+" "+blocSize);
			if(delete)
				{
				log.debug("	delete");
				sync.delete(uuid, fd.getName());
				}
			if(blocOff.size()==fd.getBlocCount())
				return true; // file doesnt change
			if(!sync.startAppend(uuid, fd.getName()))
				return false;
			RandomAccessFile ram=new RandomAccessFile(file, "r");
			byte[] b=new byte[2048];

			ByteBuffer bbuf=ByteBuffer.wrap(b);

			if(blocOff.size()<fd.getBlocCount()*.6)
				{
				log.debug("	new");

				int read;
				while ((read=ram.read(b))>0)
					{
					bbuf.limit(read);
					sync.appendData(uuid, bbuf);
					}
				}
			else
				{
				long last=0;
				Integer bloc=null;
				int count=0;
				for(long off:blocOff.keySet())
					{
					if(off==last)
						{ // ok keep this bloc from org file
						if(bloc==null)
							bloc=blocOff.get(off);
						count++;
						last+=blocSize;
						}
					else if(off>last) // new data
						{
						if(bloc!=null)
							{
							sync.appendBloc(uuid, bloc, count);
							bloc=null;
							count=0;
							}
						long d=off-last;
						log.debug("	new data "+d);

						ram.seek(last);
						while (d>0)
							{
							int read=ram.read(b, 0, (int)Math.min(b.length, d));
							log.debug(" "+d+" "+read);
							bbuf.limit(read);
							d-=read;
							sync.appendData(uuid, bbuf);
							}
						last=off;
						}
					}
				if(bloc!=null)
					{
					sync.appendBloc(uuid, bloc, count);
					bloc=null;
					count=0;
					}
				if(last<file.length())
					{
					long d=file.length()-last;
					log.debug("	new fdata "+d);
					ram.seek(last);
					while (d>0)
						{
						int read=ram.read(b, 0, (int)Math.min(b.length, d));
						bbuf.limit(read);
						d-=read;
						sync.appendData(uuid, bbuf);
						}
					}
				}
			ram.close();
			Object o=sync.endAppend(uuid, expectedHash);
			if(o instanceof FileDesc)
				{
				fd=(FileDesc)o;
				return false;
				}
			return (Boolean)o;
			}
		}

	public void setListener(UpdateListener listener)
		{
		this.listener=listener;
		}

	public static void main(String arg[]) throws Exception
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
		if(login!=null)
			{
			if(!cl.login(login, pass==null?"":pass))
				{
				log.error("login fail");
				return;
				}
			}
		String str=args.left();

		if(str.equalsIgnoreCase("update"))
			log.error("return: "+cl.update(args.left()));
		else if(str.equalsIgnoreCase("commit"))
			log.error("return: "+cl.commit(args.left()));
		else
			log.error("invalid command");
		cl.close();
		}
	}
