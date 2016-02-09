package unknow.sync;

import java.io.*;
import java.nio.*;
import java.security.*;
import java.util.*;

import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.*;

public class CommitProcessor
	{
	public static void commit(SyncClient client, FileDesc local, FileDesc server)
		{
		Map<Integer,List<IndexedHash>> hash=new HashMap<>();
		for(int i=0; i<server.getBlocs().size(); i++)
			{
			Bloc b=server.getBlocs().get(i);
			List<IndexedHash> list=hash.get(b.getRoll());
			if(list==null)
				{
				list=new ArrayList<IndexedHash>(1);
				hash.put(b.getRoll(), list);
				}
			list.add(new IndexedHash(i, b.getHash()));
			}

		Map<Long,Integer> blocFound=new HashMap<Long,Integer>();
		File file=new File(client.path.toFile(), local.getName());
		try
			{
			boolean done=false;
			do
				{
				blocFound.clear();

				if(client.listener!=null)
					client.listener.startCheckFile(local.getName());
				findBloc(client.blocSize, hash, blocFound, local, server, file);
				if(client.listener!=null)
					client.listener.doneCheckFile(local.getName());

				if(client.listener!=null)
					client.listener.startReconstruct(local.getName());
				FileDesc n=sendReconstruct(client, blocFound, server, local.getFileHash(), file);
				if(client.listener!=null)
					client.listener.doneReconstruct(local.getName(), file.length(), n==null);
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
		}

	private static void findBloc(int blocSize, Map<Integer,List<IndexedHash>> hash, Map<Long,Integer> blocFound, FileDesc local, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
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
			Bloc last=server.getBlocs().get(server.getBlocs().size()-1);
			for(int i=0; i<blocSize; i++)
				{
				int r=rcs.append((byte)0);
				if(r==last.getRoll())
					{ // found match
					Hash h=new Hash(md.digest(rcs.buf()));
					if(h.equals(last.getHash()))
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

	public static FileDesc sendReconstruct(SyncClient client, Map<Long,Integer> blocFound, FileDesc server, Hash expectedHash, File file) throws IOException
		{
		if(blocFound.size()==server.getBlocs().size())
			return null; // file doesnt change
		client.startAppend(server.getName());

		RandomAccessFile ram=new RandomAccessFile(file, "r");
		byte[] b=new byte[2048];

		ByteBuffer bbuf=ByteBuffer.wrap(b);

		if(blocFound.size()<server.getBlocs().size()*.6)
			{
			int read;
			while ((read=ram.read(b))>0)
				{
				bbuf.limit(read);
				client.appendData(bbuf);
				}
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
						bbuf.limit(read);
						d-=read;
						client.appendData(bbuf);
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
					bbuf.limit(read);
					d-=read;
					client.appendData(bbuf);
					}
				}
			}
		ram.close();
		return client.endAppend(expectedHash);
		}

	public static void send(SyncClient client, FileDesc local) throws IOException
		{
		FileDesc server=null;
		if(client.listener!=null)
			client.listener.startReconstruct(local.getName());
		client.startAppend(local.getName());
		long fileSize=0;
		try (FileInputStream fis=new FileInputStream(client.path.resolve(local.getName()).toFile()))
			{
			byte[] buf=new byte[128*1024];
			ByteBuffer bbuf=ByteBuffer.wrap(buf);
			int l;
			while ((l=fis.read(buf))>0)
				{
				fileSize+=l;
				bbuf.limit(l);
				client.appendData(bbuf);
				}
			server=client.endAppend(local.getFileHash());
			}
		if(client.listener!=null)
			client.listener.doneReconstruct(local.getName(), fileSize, server==null);
		if(server!=null)
			commit(client, local, server);
		}
	}
