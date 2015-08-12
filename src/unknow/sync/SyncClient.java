package unknow.sync;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

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
		ProjectInfo info=sync.selectProject(uuid, project);
		blocSize=info.getBlocSize();
		log.debug("bloc size: "+blocSize);
		log.debug("file count: "+info.getFileDescs().size());

		for(FileDesc f:info.getFileDescs())
			{
			UpdateDesc updateDesc=new UpdateDesc(f);
			updateDesc.start();
			}
		threadGroup.join();
		return 0;
		}

	public int commit(String project) throws NoSuchAlgorithmException, IOException, InterruptedException
		{
		ProjectInfo info=sync.selectProject(uuid, project);
		blocSize=info.getBlocSize();

		Set<FileDesc> set=new HashSet<FileDesc>();
		Common.load(set, path, "", blocSize);

		List<FileDesc> fileDescs=info.getFileDescs();
		System.out.println("file count: "+fileDescs.size()+" / "+set.size());
		l1: for(FileDesc desc:fileDescs)
			{
			Iterator<FileDesc> it=set.iterator();
			while (it.hasNext())
				{
				FileDesc fd=it.next();
				if(fd.getName().toString().contentEquals(desc.getName()))
					{
					CommitDesc commitDesc=new CommitDesc(desc);
					commitDesc.start();
					it.remove();
					continue l1;
					}
				}
			System.out.println("delete '"+desc.getName()+"'");
			synchronized (sync)
				{
				sync.delete(uuid, desc.getName());
				}
			}
		for(FileDesc fd:set)
			{
			synchronized (sync)
				{
				File f=new File(path, fd.getName().toString());
				FileInputStream fis=new FileInputStream(f);
				System.out.println("adding new file "+fd.getName());
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
			try
				{
				boolean done;
				do
					{
					blocFound.clear();
					long start=System.currentTimeMillis();
					found();
					log.debug("computing hash %s took %d ms", fd.getName(), System.currentTimeMillis()-start);
					done=reconstruct();
					long d=System.currentTimeMillis()-start;
					log.info("updating %s took %d ms (%.3f Ko/sec)", fd.getName(), d, 1000.*file.length()/(8.*d));
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

		public void found() throws IOException, NoSuchAlgorithmException
			{
			try
				{
				log.info("start check "+fd.getName());
				FileInputStream fis=new FileInputStream(file);
				RollingChecksum rcs=new RollingChecksum(blocSize);

				MessageDigest md=MessageDigest.getInstance("SHA-512");

				/** read first block */
				int c;
				for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
					rcs.append((byte)c);
				long off=0;
				while ((c=fis.read())!=-1)
					{
					int r=rcs.append((byte)c);
					for(int j=0; j<fd.getRoll().size(); j++)
						{
						if(r==fd.getRoll().get(j))
							{ // found match
							if(Arrays.equals(md.digest(rcs.buf()), fd.getHash().get(j).bytes()))
								blocFound.put(j, off);
							md.reset();
							}
						}
					off++;
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
			System.out.println("start reconstruct");

			File tmp=File.createTempFile("sync", file.getName());
			try (FileOutputStream fos=new FileOutputStream(tmp))
				{
				MessageDigest md=MessageDigest.getInstance("SHA-512");

				if(blocFound.size()<fd.getBlocCount()*.6)
					{ // download full file
					System.out.println("'"+fd.getName()+"' new");
					ByteBuffer data=sync.getFile(uuid, fd.getName());
					if(data==null)
						{
						System.out.println("failed to get file");
						return false;
						}
					byte buf[]=new byte[2048];
					do
						{
						if(data.limit()>buf.length)
							buf=new byte[data.limit()];
						data.get(buf, 0, data.limit());
						fos.write(buf, 0, data.limit());
						md.update(buf, 0, data.limit());

						data=sync.getNext(uuid);
						}
					while (data!=null);
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
								System.out.println("read bloc "+i+" from "+fd.getName());
								System.out.println(blocFound.get(i));
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
								System.out.println("read bloc "+i+" from serv");
								ByteBuffer bloc=sync.getBloc(uuid, fd.getName(), i);
								if(bloc==null)
									{
									System.out.println("failed to get bloc");
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
				System.out.println("got: "+StringTools.toHex(digest));
				System.out.println("exp: "+StringTools.toHex(fd.getFileHash().bytes()));
				return Arrays.equals(digest, fd.getFileHash().bytes());
				}
			}
		}

	private class CommitDesc extends Thread
		{
		private FileDesc fd;

		private Map<Long,Integer> blocOff=new TreeMap<Long,Integer>();
		private boolean delete=false;

		public CommitDesc(FileDesc f)
			{
			super(threadGroup, "Commit "+f.getName());
			fd=f;
			}

		public void run()
			{
			try
				{
				boolean done;
				do
					{
					found();
					synchronized (sync)
						{
						done=sendReconstruct();
						}
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

		private void found() throws IOException, NoSuchAlgorithmException
			{
			try
				{
				System.out.println("start check "+fd.getName());
				FileInputStream fis=new FileInputStream(new File(path, fd.getName().toString()));
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
					for(int j=0; j<fd.getRoll().size(); j++)
						{
						if(r==fd.getRoll().get(j))
							{ // found match
							Hash h=new Hash(md.digest(rcs.buf()));
							md.reset();
							if(h.equals(fd.getHash().get(j)))
								{
								blocOff.put(off, j);

								for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
									rcs.append((byte)c);
								off+=blocSize;
								continue loop;
								}
							}
						}
					off++;
					}
				// test with padding
				int lastRoll=fd.getRoll().get(fd.getRoll().size()-1);
				for(int i=0; i<blocSize; i++)
					{
					int r=rcs.append((byte)0);
					if(r==lastRoll)
						{ // found match
						Hash h=new Hash(md.digest(rcs.buf()));
						if(h.equals(fd.getHash().get(fd.getRoll().size()-1)))
							blocOff.put(off, fd.getRoll().size()-1);
						md.reset();
						}
					off++;
					}
				System.out.println(fd.getName()+"	bloc found: "+blocOff.size()+"/"+fd.getBlocCount());
				fis.close();
				}
			catch (FileNotFoundException e)
				{
				delete=true;
				}
			}

		public boolean sendReconstruct() throws IOException
			{
			System.out.println("start reconstruct "+fd.getName()+" "+blocSize);
			if(delete)
				{
				System.out.println("	delete");
				sync.delete(uuid, fd.getName());
				}
			if(blocOff.size()==fd.getBlocCount())
				return true; // file doesnt change
			File f=new File(path, fd.getName().toString());
			RandomAccessFile ram=new RandomAccessFile(f, "r");
			sync.startAppend(uuid, fd.getName());
			byte[] b=new byte[2048];

			ByteBuffer bbuf=ByteBuffer.wrap(b);

			if(blocOff.size()==0) // no bloc found file totaly changed
				{
				System.out.println("	new");

				int read;
				while ((read=ram.read(b))>0)
					{
					bbuf.limit(read);
					sync.appendData(uuid, bbuf);
					}
				}
			else
				{
//				long[] off=new long[blocOff.size()];
//				int i=0;
//				for(long l:blocOff.keySet())
//					{
//					int j=0;
//					while (j<i&&l>off[j])
//						j++;
//					for(int k=i; k>j; k--)
//						off[k]=off[k-1];
//					off[j]=l;
//					System.out.println(j+"/"+i+": "+l);
//					i++;
//					}
//				for(i=0; i<off.length; i++)
//					System.out.println(off[i]);

				long last=0;
				Integer bloc=null;
				int count=0;
				for(long off:blocOff.keySet())
					{
					if(off<last) // TODO can be optimised
						{
						System.out.println("bloc over lap");
						}
					else if(off==last)
						{ // ok keep this bloc from org file
						if(bloc==null)
							bloc=blocOff.get(off);
						count++;
						last+=blocSize;
						}
					else
						// new data
						{
						if(bloc!=null)
							{
							sync.appendBloc(uuid, bloc, count);
							bloc=null;
							count=0;
							}
						long d=off-last;
						System.out.println("	new data "+d);

						ram.seek(last);
						while (d>0)
							{
							int read=ram.read(b, 0, (int)Math.min(b.length, d));
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
				if(last<f.length())
					{
					long d=f.length()-last;
					System.out.println("	new fdata "+d);
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
			Object o=sync.endAppend(uuid, fd.getFileHash());
			if(o instanceof FileDesc)
				{
				fd=(FileDesc)o;
				return false;
				}
			return (Boolean)o;
			}
		}

	public static void main(String arg[]) throws Exception
		{
		System.out.println(Arrays.toString(arg));
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
				System.out.println("usage: <option> <cmd> <project>");
				System.out.println("Option");
				System.out.println("	-H <host> |--host=<host>");
				System.out.println("		Set the server host to use (required).");
				System.out.println("	-P <port> | --port=<port>");
				System.out.println("		Set the port to use, use the default port if not specified");
				System.out.println("	-l <login> | --login=<login>");
				System.out.println("		Set login.");
				System.out.println("	-p <pass> | --pass=<pass>");
				System.out.println("	-d <path> | --path=<path>");
				System.out.println("	--help");
				System.out.println("		This help.");
				System.out.println("Command");
				System.out.println("	update");
				System.out.println("	commit");
				return;
				}
			}
		if(host==null)
			{
			System.out.println("host required");
			return;
			}
		SyncClient cl=new SyncClient(host, port, path);
		if(login!=null)
			{
			if(!cl.login(login, pass==null?"":pass))
				{
				System.out.println("login fail");
				return;
				}
			}
		String str=args.left();

		if(str.equalsIgnoreCase("update"))
			System.out.println("return: "+cl.update(args.left()));
		else if(str.equalsIgnoreCase("commit"))
			System.out.println("return: "+cl.commit(args.left()));
		else
			System.out.println("invalid command");
		cl.close();
		}
	}
