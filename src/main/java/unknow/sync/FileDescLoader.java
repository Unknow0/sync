package unknow.sync;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.*;

import unknow.sync.proto.*;

public class FileDescLoader
	{
	private static final Logger log=LogManager.getFormatterLogger(FileDescLoader.class);
	private static final ExecutorService exec=Executors.newCachedThreadPool();

	public static void load(Collection<FileDesc> files, Path root, int blocSize) throws IOException
		{
		Visitor visitor=new Visitor(root, blocSize);
		Files.walkFileTree(root, visitor);
		for(Future<FileDesc> future:visitor.futures())
			{
			try
				{
				files.add(future.get());
				}
			catch (Exception e)
				{
				log.error(e);
				}
			}
		}

	/**
	 * @return matching bloc B bloc -> A bloc
	 */
	public static Map<Integer,Integer> diff(FileDesc a, FileDesc b)
		{
		Map<Integer,List<IndexedHash>> blocA=new HashMap<Integer,List<IndexedHash>>();
		int i=0;
		for(Bloc bloc:a.getBlocs())
			{
			List<IndexedHash> l=blocA.get(bloc.getRoll());
			if(l==null)
				{
				l=new ArrayList<IndexedHash>();
				blocA.put(bloc.getRoll(), l);
				}
			l.add(new IndexedHash(i++, bloc.getHash()));
			}

		Map<Integer,Integer> map=new HashMap<Integer,Integer>();
		loop: for(i=0; i<b.getBlocs().size(); i++)
			{
			Bloc blocB=b.getBlocs().get(i);
			List<IndexedHash> list=blocA.get(blocB.getRoll());
			if(list!=null)
				{
				for(IndexedHash bloc:list)
					{
					if(blocB.getHash().equals(bloc.h))
						{
						map.put(i, bloc.i);
						continue loop;
						}
					}
				}
			}
		return map;
		}

	public static FileDesc loadFile(Path root, Path file, int blocSize) throws NoSuchAlgorithmException, FileNotFoundException, IOException
		{
		MessageDigest md=MessageDigest.getInstance("SHA-512");
		MessageDigest fileMd=MessageDigest.getInstance("SHA-512");

		FileDesc desc=new FileDesc(root.relativize(file).toString(), new ArrayList<Bloc>(), null);
		try (FileInputStream fis=new FileInputStream(file.toString()))
			{
			byte[] buf=new byte[blocSize];
			byte[] h;
			int s;
			while ((s=fis.read(buf))!=-1)
				{
				int c;
				while (s<buf.length&&(c=fis.read())!=-1)
					buf[s++]=(byte)c;
				fileMd.update(buf, 0, s);

				// padding
				while (s<buf.length)
					buf[s++]=0;
				h=md.digest(buf);
				Bloc bloc=new Bloc(RollingChecksum.compute(buf), new Hash(h));
				desc.getBlocs().add(bloc);
				}
			h=fileMd.digest();
			desc.setFileHash(new Hash(h));
			}
		return desc;
		}

	private static class Visitor implements FileVisitor<Path>
		{
		private Path root;
		private int blocSize;
		private List<Future<FileDesc>> futures=new ArrayList<>();

		public Visitor(Path root, int blocFile)
			{
			this.root=root;
			this.blocSize=blocFile;
			}

		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
			return FileVisitResult.CONTINUE;
			}

		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
			if(attrs.isRegularFile())
				futures.add(exec.submit(new FileLoader(root, file, blocSize)));
			return FileVisitResult.CONTINUE;
			}

		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
			{
			return FileVisitResult.CONTINUE;
			}

		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
			return FileVisitResult.CONTINUE;
			}

		public List<Future<FileDesc>> futures()
			{
			return futures;
			}
		}

	private static class FileLoader implements Callable<FileDesc>
		{
		private Path file;
		private int blocSize;
		private Path root;

		public FileLoader(Path root, Path file, int blocSize) throws IOException
			{
			this.root=root;
			this.file=file;
			this.blocSize=blocSize;
			}

		public FileDesc call() throws Exception
			{
			return loadFile(root, file, blocSize);
			}
		}

	public static class IndexedHash
		{
		public int i;
		public Hash h;

		public IndexedHash(int i, Hash h)
			{
			this.i=i;
			this.h=h;
			}
		}

//	public static void main(String[] arg) throws Exception
//		{
//		Set<FileDesc> set=new ArraySet<FileDesc>(10, 2);
//
//		long t=System.currentTimeMillis();
//		load(set, "/usr/lib/jvm", "", 1024);
//		System.out.println("computing hash: "+(System.currentTimeMillis()-t)+" ms");
//
////		set.clear();
////
////		t=System.currentTimeMillis();
////		load(set, "/usr/lib/jvm", "", 1024);
////		System.out.println("computing hash: "+(System.currentTimeMillis()-t)+" ms");
//
//		FileDesc next=set.iterator().next();
//		log.info(next);
//		}
	}
