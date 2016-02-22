package unknow.sync;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.logging.log4j.*;

import unknow.common.data.*;
import unknow.sync.proto.pojo.*;

public class FileDescLoader
	{
	private static final Logger log=LogManager.getFormatterLogger(FileDescLoader.class);
	private static final ExecutorService exec=Executors.newCachedThreadPool();

	public static void load(Collection<FileDesc> files, Path root, int blocSize, Pattern pattern) throws IOException
		{
		root=root.toAbsolutePath().normalize();
		Visitor visitor=new Visitor(root, blocSize, pattern);
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
		for(Bloc bloc:a.blocs)
			{
			List<IndexedHash> l=blocA.get(bloc.roll);
			if(l==null)
				{
				l=new ArrayList<IndexedHash>();
				blocA.put(bloc.roll, l);
				}
			l.add(new IndexedHash(i++, bloc.hash));
			}

		Map<Integer,Integer> map=new HashMap<Integer,Integer>();
		loop: for(i=0; i<b.blocs.length; i++)
			{
			Bloc blocB=b.blocs[i];
			List<IndexedHash> list=blocA.get(blocB.roll);
			if(list!=null)
				{
				for(IndexedHash bloc:list)
					{
					if(blocB.hash.equals(bloc.h))
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

		FileDesc desc=new FileDesc(file.toString(), null, null);
		List<Bloc> list=new ArrayList<Bloc>();
		try (FileInputStream fis=new FileInputStream(root.resolve(file).toString()))
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
				byte p=0;
				while (s<buf.length)
					buf[s++]=++p;
				h=md.digest(buf);
				Bloc bloc=new Bloc(RollingChecksum.compute(buf), new Hash(h));
				list.add(bloc);
				}
			h=fileMd.digest();
			desc.fileHash=new Hash(h);
			desc.blocs=list.toArray(new Bloc[0]);
			}
		return desc;
		}

	private static class Visitor implements FileVisitor<Path>
		{
		private Path root;
		private int blocSize;
		private Matcher m;
		private List<Future<FileDesc>> futures=new ArrayList<>();

		public Visitor(Path root, int blocFile, Pattern pattern)
			{
			this.root=root;
			this.blocSize=blocFile;
			this.m=pattern==null?null:pattern.matcher("");
			}

		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
			if(m!=null)
				m.reset(root.relativize(dir).toString());
			return m==null||m.matches()||m.hitEnd()?FileVisitResult.CONTINUE:FileVisitResult.SKIP_SUBTREE;
			}

		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
			if(attrs.isRegularFile())
				{
				if(m!=null)
					m.reset(root.relativize(file).toString());
				if(m==null||m.matches())
					futures.add(exec.submit(new FileLoader(root, file, blocSize)));
				}
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
			return loadFile(root, root.relativize(file), blocSize);
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
	}
