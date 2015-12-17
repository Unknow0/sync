package unknow.sync;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.*;

public class UpdateProcessor
	{
	public static void update(SyncClient client, FileDesc local, FileDesc server) throws NoSuchAlgorithmException, IOException
		{
		if(client.listener!=null)
			client.listener.startFile(local.getName());
		Map<Integer,Long> blocFound=new HashMap<Integer,Long>();

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
		File file=new File(client.path.toFile(), local.getName());
		boolean done;
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
			if(blocFound.size()>server.getBlocs().size()*.4)
				done=reconstruct(client, blocFound, server, file);
			else
				done=client.getFile(file, server.getName(), server.getFileHash());
			if(client.listener!=null)
				client.listener.doneReconstruct(local.getName(), file.length(), done);
			if(!done)
				local=FileDescLoader.loadFile(client.path, Paths.get(local.getName()), client.blocSize);
			}
		while (!done);

		if(client.listener!=null)
			client.listener.doneFile(local.getName(), file.length());
		}

	private static void findBloc(int blocSize, Map<Integer,List<IndexedHash>> hash, Map<Integer,Long> blocFound, FileDesc local, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
		{
		Map<Integer,Integer> diff=FileDescLoader.diff(local, server);
		for(Map.Entry<Integer,Integer> e:diff.entrySet())
			blocFound.put(e.getKey(), (long)e.getValue()*blocSize);
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
			while ((c=fis.read())!=-1)
				{
				int r=rcs.append((byte)c);
				List<IndexedHash> list=hash.get(r);
				if(list!=null)
					{ // found match
					for(IndexedHash p:list)
						{
						md.reset();
						if(Arrays.equals(md.digest(rcs.buf()), p.h.bytes()))
							{
							blocFound.put(p.i, off);
							break;
							}
						}
					}
				off++;
				}
			// test with padding
			Bloc last=server.getBlocs().get(server.getBlocs().size()-1);
			for(int i=0; i<blocSize-1; i++)
				{
				int r=rcs.append((byte)0);
				if(r==last.getRoll())
					{ // found match
					md.reset();
					if(Arrays.equals(md.digest(rcs.buf()), last.getHash().bytes()))
						blocFound.put(server.getBlocs().size()-1, off);
					}
				off++;
				}

			fis.close();
			}
		catch (FileNotFoundException e)
			{
			}
		}

	private static boolean reconstruct(SyncClient client, Map<Integer,Long> blocFound, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
		{
		Integer[] blocs=blocFound.keySet().toArray(new Integer[0]);
		Arrays.sort(blocs);

		MessageDigest md=MessageDigest.getInstance("SHA-512");
		File tmp=File.createTempFile("sync_", ".tmp");
		try (FileOutputStream fos=new FileOutputStream(tmp);
				RandomAccessFile ram=new RandomAccessFile(file, "r");)
			{

			int bi=0;
			int i=0;
			byte[] buf=new byte[client.blocSize];

			int bc=server.getBlocs().size();
			while (i<bc)
				{
				int len=blocs[bi]-i;
				if(len>0) // blocs need to be retreiving from server
					{
					ByteBuffer bb=client.getBloc(server.getName(), i, len);
					byte[] array=new byte[bb.remaining()];
					bb.get(array);
					fos.write(array);
					md.update(array);
					i+=len;
					}
				ram.seek(blocFound.get(i));
				int c=0;
				while (i<bc&&blocs[bi]==i)
					{
					int s=ram.read(buf);
					while (s<client.blocSize&&(c=ram.read())!=-1)
						buf[s++]=(byte)c;

					fos.write(buf, 0, s);
					md.update(buf, 0, s);
					bi++;
					i++;
					}
				}
			}

		byte[] digest=md.digest();

		try (FileInputStream fis=new FileInputStream(tmp))
			{
			byte[] buf=new byte[1024];
			int l;
			while ((l=fis.read(buf))>=0)
				md.update(buf, 0, l);
			}

		Files.move(Paths.get(tmp.getPath()), Paths.get(file.getPath()), StandardCopyOption.REPLACE_EXISTING);

		return Arrays.equals(server.getFileHash().bytes(), digest);
		}
	}
