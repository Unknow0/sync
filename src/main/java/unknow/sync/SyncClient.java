package unknow.sync;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

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

	public void update(String login, String pass, String project, boolean delete) throws IOException, InterruptedException, NoSuchAlgorithmException
		{
		LoginRes res=sync.login(login, pass, project, Action.read);
		uuid=res.getUuid();
		ProjectInfo info=res.getProject();

		blocSize=info.getBlocSize();

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize);

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.getHashs().size()/.75));
		for(int i=0; i<info.getHashs().size(); i++)
			map.put(info.getHashs().get(i).getName(), new IndexedHash(i, info.getHashs().get(i).getHash()));

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

	public void commit(String login, String pass, String project) throws NoSuchAlgorithmException, IOException, InterruptedException
		{
		LoginRes res=sync.login(login, pass, project, Action.write);

		uuid=res.getUuid();
		ProjectInfo info=res.getProject();

		blocSize=info.getBlocSize();

		Set<FileDesc> files=new HashSet<FileDesc>();
		FileDescLoader.load(files, path, blocSize);

		Map<String,IndexedHash> map=new HashMap<String,IndexedHash>((int)Math.ceil(info.getHashs().size()/.75));
		for(int i=0; i<info.getHashs().size(); i++)
			map.put(info.getHashs().get(i).getName(), new IndexedHash(i, info.getHashs().get(i).getHash()));

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
					CommitProcessor.commit(this, local, server);

					it.remove();
					break;
					}
				}
			}

		for(FileDesc local:fileToCreate)
			CommitProcessor.send(this, local);
		
		for(Map.Entry<String,IndexedHash> e:map.entrySet())
			sync.delete(uuid, e.getKey());

//		List<FileDesc> fileDescs=info.getFileDescs();
//		log.debug("file count: "+fileDescs.size()+" / "+set.size());
//		l1: for(FileDesc desc:fileDescs)
//			{
//			if(m!=null)
//				{
//				m.reset(desc.getName());
//				if(!m.matches())
//					continue;
//				}
//			Iterator<FileDesc> it=set.iterator();
//			while (it.hasNext())
//				{
//				FileDesc fd=it.next();
//				if(fd.getName().toString().contentEquals(desc.getName()))
//					{
//					CommitDesc commitDesc=new CommitDesc(desc, fd.getFileHash());
//					commitDesc.start();
//					it.remove();
//					continue l1;
//					}
//				}
//			log.debug("delete '"+desc.getName()+"'");
//			synchronized (sync)
//				{
//				sync.delete(uuid, desc.getName());
//				}
//			}
//		for(FileDesc fd:set)
//			{
//			if(m!=null)
//				{
//				m.reset(fd.getName());
//				if(!m.matches())
//					continue;
//				}
//			synchronized (sync)
//				{
//				File f=new File(path, fd.getName().toString());
//				FileInputStream fis=new FileInputStream(f);
//				log.debug("adding new file "+fd.getName());
//				sync.startAppend(uuid, fd.getName());
//
//				byte[] b=new byte[2048];
//				ByteBuffer bbuf=ByteBuffer.wrap(b);
//
//				int r;
//				while ((r=fis.read(b))>0)
//					{
//					bbuf.limit(r);
//					sync.appendData(uuid, bbuf);
//					}
//				sync.endAppend(uuid, fd.getFileHash()); // TODO check values
//
//				fis.close();
//				}
//			}
//		threadGroup.join();
//		return 0;
		}

	public void close()
		{
		client.close();
		}

	public void findBlocCommit(Map<Integer,List<IndexedHash>> hash, Map<Long,Integer> blocFound, FileDesc local, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
		{
		Map<Integer,Integer> diff=FileDescLoader.diff(local, server);
		for(Map.Entry<Integer,Integer> e:diff.entrySet())
			blocFound.put((long)e.getValue()*blocSize, e.getKey());
		if(blocFound.size()>server.getBlocs().size()*.6)
			return;

//			List<Integer> b=new ArrayList<Integer>(diff.values());
//			Collections.sort(b);
		// TODO Opti with already found bloc?

		try
			{ // TODO can i opitmize this?
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

				List<IndexedHash> list=hash.get(r);
				if(list!=null)
					{ // found match
					Hash h=new Hash(md.digest(rcs.buf()));
					for(IndexedHash p:list)
						{
						if(h.equals(p.h))
							{
							blocFound.put(off, p.i);

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
			Bloc b=server.getBlocs().get(server.getBlocs().size()-1);
			for(int i=0; i<blocSize; i++)
				{
				int r=rcs.append((byte)0);
				if(r==b.getRoll())
					{ // found match
					Hash h=new Hash(md.digest(rcs.buf()));
					if(h.equals(b.getHash()))
						{
						blocFound.put(off, server.getBlocs().size()-1);
						break;
						}
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

//	private class CommitDesc extends Thread
//		{ XXX
//		private FileDesc fd;
//		private Hash expectedHash;
//		private File file;
//
//		private Map<Long,Integer> blocOff=new TreeMap<Long,Integer>();
//		private boolean delete=false;
//
//		public CommitDesc(FileDesc f, Hash hash)
//			{
//			super(threadGroup, "Commit "+f.getName());
//			fd=f;
//			expectedHash=hash;
//			file=new File(path, fd.getName().toString());
//			}
//
//		public void run()
//			{
//			Map<Integer,P> hash=new HashMap<Integer,P>();
//			for(int i=0; i<fd.getBlocCount(); i++)
//				hash.put(fd.getRoll().get(i), new P(i, fd.getHash().get(i)));
//
//			try
//				{
//				boolean done;
//				do
//					{
//					long start=System.currentTimeMillis();
//					found(hash);
//					log.debug("computing hash %s took %d ms", fd.getName(), System.currentTimeMillis()-start);
//					synchronized (sync)
//						{
//						done=sendReconstruct();
//						}
//					long d=System.currentTimeMillis()-start;
//					log.info("updating %s took %d ms (%.3f Ko/sec)", fd.getName(), d, 1000.*file.length()/(8.*d));
//					log.debug("done: "+done);
//					}
//				while (!done);
//				}
//			catch (NoSuchAlgorithmException e)
//				{
//				e.printStackTrace();
//				}
//			catch (IOException e)
//				{
//				e.printStackTrace();
//				}
//			}
//
//		private void found(Map<Integer,P> hash) throws IOException, NoSuchAlgorithmException
//			{
//			try
//				{
//				log.debug("start check "+fd.getName());
//				FileInputStream fis=new FileInputStream(file);
//				RollingChecksum rcs=new RollingChecksum(blocSize);
//
//				MessageDigest md=MessageDigest.getInstance("SHA-512");
//
//				/** read first block */
//				int c;
//				for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
//					rcs.append((byte)c);
//				long off=0;
//				loop: while ((c=fis.read())!=-1)
//					{
//					int r=rcs.append((byte)c);
//
//					P p=hash.get(r);
//					if(p!=null)
//						{ // found match
//						Hash h=new Hash(md.digest(rcs.buf()));
//						md.reset();
//						if(h.equals(p.hash))
//							{
//							blocOff.put(off, p.i);
//
//							for(int i=0; i<blocSize-1&&(c=fis.read())!=-1; i++)
//								rcs.append((byte)c);
//							off+=blocSize;
//							continue loop;
//							}
//						}
//					off++;
//					}
//				// test with padding
//				int lastRoll=fd.getRoll().get(fd.getRoll().size()-1);
//				Hash lastHash=fd.getHash().get(fd.getRoll().size()-1);
//				for(int i=0; i<blocSize; i++)
//					{
//					int r=rcs.append((byte)0);
//					if(r==lastRoll)
//						{ // found match
//						Hash h=new Hash(md.digest(rcs.buf()));
//						if(h.equals(lastHash))
//							{
//							blocOff.put(off, fd.getRoll().size()-1);
//							break;
//							}
//						md.reset();
//						}
//					off++;
//					}
//				log.debug(fd.getName()+"	bloc found: "+blocOff.size()+"/"+fd.getBlocCount());
//				fis.close();
//				}
//			catch (FileNotFoundException e)
//				{
//				delete=true;
//				}
//			}
//
//		public boolean sendReconstruct() throws IOException
//			{
//			log.debug("start reconstruct "+fd.getName()+" "+blocSize);
//			if(delete)
//				{
//				log.debug("	delete");
//				sync.delete(uuid, fd.getName());
//				}
//			if(blocOff.size()==fd.getBlocCount())
//				return true; // file doesnt change
//			if(!sync.startAppend(uuid, fd.getName()))
//				return false;
//			RandomAccessFile ram=new RandomAccessFile(file, "r");
//			byte[] b=new byte[2048];
//
//			ByteBuffer bbuf=ByteBuffer.wrap(b);
//
//			if(blocOff.size()<fd.getBlocCount()*.6)
//				{
//				log.debug("	new");
//
//				int read;
//				while ((read=ram.read(b))>0)
//					{
//					bbuf.limit(read);
//					sync.appendData(uuid, bbuf);
//					}
//				}
//			else
//				{
//				long last=0;
//				Integer bloc=null;
//				int count=0;
//				for(long off:blocOff.keySet())
//					{
//					if(off==last)
//						{ // ok keep this bloc from org file
//						if(bloc==null)
//							bloc=blocOff.get(off);
//						count++;
//						last+=blocSize;
//						}
//					else if(off>last) // new data
//						{
//						if(bloc!=null)
//							{
//							sync.appendBloc(uuid, bloc, count);
//							bloc=null;
//							count=0;
//							}
//						long d=off-last;
//						log.debug("	new data "+d);
//
//						ram.seek(last);
//						while (d>0)
//							{
//							int read=ram.read(b, 0, (int)Math.min(b.length, d));
//							log.debug(" "+d+" "+read);
//							bbuf.limit(read);
//							d-=read;
//							sync.appendData(uuid, bbuf);
//							}
//						last=off;
//						}
//					}
//				if(bloc!=null)
//					{
//					sync.appendBloc(uuid, bloc, count);
//					bloc=null;
//					count=0;
//					}
//				if(last<file.length())
//					{
//					long d=file.length()-last;
//					log.debug("	new fdata "+d);
//					ram.seek(last);
//					while (d>0)
//						{
//						int read=ram.read(b, 0, (int)Math.min(b.length, d));
//						bbuf.limit(read);
//						d-=read;
//						sync.appendData(uuid, bbuf);
//						}
//					}
//				}
//			ram.close();
//			Object o=sync.endAppend(uuid, expectedHash);
//			if(o instanceof FileDesc)
//				{
//				fd=(FileDesc)o;
//				return false;
//				}
//			return (Boolean)o;
//			}
//		}

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
				cl.update(login, pass, args.left(), false);
			else if(str.equalsIgnoreCase("commit"))
				cl.commit(login, pass, args.left());
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
