package unknow.sync;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import unknow.common.data.*;
import unknow.sync.FileDescLoader.IndexedHash;
import unknow.sync.proto.pojo.*;

public class UpdateProcessor
	{
	public static void update(SyncClient client, FileDesc local, FileDesc server) throws NoSuchAlgorithmException, IOException, SyncException
		{
		if(client.listener!=null)
			client.listener.startFile(local.name);
		Map<Integer,Long> blocFound=new HashMap<Integer,Long>();

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
		File file=new File(client.path.toFile(), local.name);
		boolean done;
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
			if(blocFound.size()>server.blocs.length*.4)
				done=reconstruct(client, blocFound, server, file);
			else
				done=client.getFile(file, server.name, server.fileHash);
			if(client.listener!=null)
				client.listener.doneReconstruct(local.name, file.length(), done);
			if(!done)
				local=FileDescLoader.loadFile(client.path, Paths.get(local.name), client.blocSize);
			}
		while (!done);

		if(client.listener!=null)
			client.listener.doneFile(local.name, file.length());
		}

	private static void findBloc(int blocSize, Map<Integer,List<IndexedHash>> hash, Map<Integer,Long> blocFound, FileDesc local, FileDesc server, File file) throws IOException, NoSuchAlgorithmException
		{
		Map<Integer,Integer> diff=FileDescLoader.diff(local, server);
		for(Map.Entry<Integer,Integer> e:diff.entrySet())
			blocFound.put(e.getKey(), (long)e.getValue()*blocSize);
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
			while ((c=fis.read())!=-1)
				{
				int r=rcs.append((byte)c);
				List<IndexedHash> list=hash.get(r);
				if(list!=null)
					{ // found match
					for(IndexedHash p:list)
						{
						md.reset();
						if(Arrays.equals(md.digest(rcs.buf()), p.h.bytes))
							{
							blocFound.put(p.i, off);
							break;
							}
						}
					}
				off++;
				}
			// test with padding
			Bloc last=server.blocs[server.blocs.length-1];
			byte p=0;
			for(int i=0; i<blocSize-1; i++)
				{
				int r=rcs.append(++p);
				if(r==last.roll)
					{ // found match
					md.reset();
					if(Arrays.equals(md.digest(rcs.buf()), last.hash.bytes))
						blocFound.put(server.blocs.length-1, off);
					}
				off++;
				}

			fis.close();
			}
		catch (FileNotFoundException e)
			{
			}
		}

	private static boolean reconstruct(SyncClient client, Map<Integer,Long> blocFound, FileDesc server, File file) throws IOException, NoSuchAlgorithmException, SyncException
		{
		Integer[] blocs=blocFound.keySet().toArray(new Integer[0]);
		Arrays.sort(blocs);

		MessageDigest md=MessageDigest.getInstance("SHA-512");
		File tmp=File.createTempFile("sync_", ".tmp");
		try (FileOutputStream fos=new FileOutputStream(tmp);
				RandomAccessFile ram=new RandomAccessFile(file, "r");)
			{
			int bi=0; // index of next bloc found
			int i=0; // index of current bloc
			byte[] buf=new byte[client.blocSize];

			int bc=server.blocs.length;
			while (i<bc&&bi<blocs.length)
				{
				int len=blocs[bi]-i;
				if(len>0) // blocs need to be retreiving from server
					{
					byte[] array=client.getBloc(server.name, i, len);
					fos.write(array);
					md.update(array);
					i+=len;
					}
				ram.seek(blocFound.get(i));
				int c=0;
				while (i<bc&&bi<blocs.length&&blocs[bi]==i) // append consecutive already found bloc
					{
					int s=ram.read(buf);
					if(s>=0)
						{
						while (s<buf.length&&(c=ram.read())!=-1)
							buf[s++]=(byte)c;

						fos.write(buf, 0, s);
						md.update(buf, 0, s);
						}
					// TODO check if we really are on the last bloc?
					bi++;
					i++;
					}
				}
			if(i<bc) // file on server has bloc remaining
				{
				byte[] array=client.getBloc(server.name, i, bc-i);
				fos.write(array);
				md.update(array);
				}
			}

		byte[] digest=md.digest();

		Files.move(Paths.get(tmp.getPath()), Paths.get(file.getPath()), StandardCopyOption.REPLACE_EXISTING);

		return Arrays.equals(server.fileHash.bytes, digest);
		}
	}
