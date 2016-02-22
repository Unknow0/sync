package unknow.sync;

import java.io.*;
import java.security.*;
import java.util.*;

import unknow.common.data.*;
import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.pojo.*;

public class CommitProcessor
	{
	/**
	 * @throws SyncException 
	 * @retrun fileSize
	 */
	public static long commit(SyncClient client, FileDesc local, FileDesc server) throws SyncException
		{
		Map<Integer,List<IndexedHash>> hash=new HashMap<>();
		for(int i=0; i<server.blocs.length; i++)
			{
			Bloc b=server.blocs[i];
			List<IndexedHash> list=hash.get(b.roll);
			if(list==null)
				{
				list=new ArrayList<IndexedHash>(1);
				hash.put(b.roll, list);
				}
			list.add(new IndexedHash(i, b.hash));
			}

		Map<Long,Integer> blocFound=new HashMap<Long,Integer>();
		File file=new File(client.path.toFile(), local.name);
		long fileSize=file.length();
		try
			{
			boolean done=false;
			do
				{
				blocFound.clear();

				if(client.listener!=null)
					client.listener.startCheckFile(local.name);
				findBloc(client.blocSize, hash, blocFound, local, server, file);
				if(client.listener!=null)
					client.listener.doneCheckFile(local.name);

				if(client.listener!=null)
					client.listener.startReconstruct(local.name);
				FileDesc n=sendReconstruct(client, blocFound, server, local.fileHash, file);
				if(client.listener!=null)
					client.listener.doneReconstruct(local.name, fileSize, n==null);
				if(n==null)
					done=true;
				else
					server=n;
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
		return fileSize;
		}

	private static void findBloc(int blocSize, Map<Integer,List<IndexedHash>> hash, Map<Long,Integer> blocFound, FileDesc local, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
		{
		Map<Integer,Integer> diff=FileDescLoader.diff(local, server);
		for(Map.Entry<Integer,Integer> e:diff.entrySet())
			blocFound.put((long)e.getValue()*blocSize, e.getKey());
		if(blocFound.size()>server.blocs.length*.6)
			return;

//			List<Integer> b=new ArrayList<Integer>(diff.values());
//			Collections.sort(b);
		// TODO Opti with already found bloc?

		try
			{ // TODO can i optimize this?
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
					md.reset();
					for(IndexedHash p:list)
						{
						if(p.h.equals(h))
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
			Bloc last=server.blocs[server.blocs.length-1];
			byte p=0;
			for(int i=0; i<blocSize; i++)
				{
				int r=rcs.append(++p);
				if(r==last.roll)
					{ // found match
					Hash h=new Hash(md.digest(rcs.buf()));
					if(h.equals(last.hash))
						{
						blocFound.put(off, server.blocs.length-1);
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

	public static FileDesc sendReconstruct(SyncClient client, Map<Long,Integer> blocFound, FileDesc server, Hash expectedHash, File file) throws IOException, SyncException
		{
		if(blocFound.size()==server.blocs.length)
			return null; // file doesnt change
		client.startAppend(server.name);

		RandomAccessFile ram=new RandomAccessFile(file, "r");
		byte[] b=new byte[2048];

		if(blocFound.size()<server.blocs.length*.6)
			{
			int read;
			while ((read=ram.read(b))>0)
				client.appendData(read==b.length?b:Arrays.copyOf(b, read));
			}
		else
			{
			long last=0;
			Integer bloc=null;
			int count=0;
			for(long off:blocFound.keySet())
				{
				if(off==last)
					{ // ok keep this bloc from org file
					if(bloc==null)
						bloc=blocFound.get(off);
					count++;
					last+=client.blocSize;
					}
				else if(off>last) // new data
					{
					if(bloc!=null)
						{
						client.appendBloc(bloc, count);
						bloc=null;
						count=0;
						}
					long d=off-last;

					ram.seek(last);
					while (d>0)
						{
						int read=ram.read(b, 0, (int)Math.min(b.length, d));
						d-=read;
						client.appendData(read==b.length?b:Arrays.copyOf(b, read));
						}
					last=off;
					}
				}
			if(bloc!=null)
				{
				client.appendBloc(bloc, count);
				bloc=null;
				count=0;
				}
			if(last<file.length())
				{
				long d=file.length()-last;
				ram.seek(last);
				while (d>0)
					{
					int read=ram.read(b, 0, (int)Math.min(b.length, d));
					d-=read;
					client.appendData(b.length==read?b:Arrays.copyOf(b, read));
					}
				}
			}
		ram.close();
		return client.endAppend(expectedHash);
		}

	/**
	 * @return fileSize
	 * @throws SyncException 
	 */
	public static long send(SyncClient client, FileDesc local) throws IOException, SyncException
		{
		long fileSize=0;
		FileDesc server=null;
		if(client.listener!=null)
			client.listener.startReconstruct(local.name);
		client.startAppend(local.name);
		try (FileInputStream fis=new FileInputStream(client.path.resolve(local.name).toFile()))
			{
			byte[] buf=new byte[128*1024];
			int l;
			while ((l=fis.read(buf))>0)
				{
				fileSize+=l;
				client.appendData(buf.length==l?buf:Arrays.copyOf(buf, l));
				}
			server=client.endAppend(local.fileHash);
			}
		if(client.listener!=null)
			client.listener.doneReconstruct(local.name, fileSize, server==null);
		if(server!=null)
			return commit(client, local, server);
		return fileSize;
		}
	}
