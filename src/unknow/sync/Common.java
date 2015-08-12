package unknow.sync;

import java.io.*;
import java.security.*;
import java.util.*;

import unknow.sync.proto.*;

public class Common
	{
	public static FileDesc load(CharSequence f, File d, int blocSize) throws FileNotFoundException, IOException, NoSuchAlgorithmException
		{
		MessageDigest md=MessageDigest.getInstance("SHA-512");
		MessageDigest fileMd=MessageDigest.getInstance("SHA-512");

		int bc=(int)(d.length()/blocSize+1);
		FileDesc desc=new FileDesc(f, (int)Math.ceil(d.length()/(double)blocSize), new ArrayList<Integer>(bc), new ArrayList<Hash>(bc), null);
		try (FileInputStream fis=new FileInputStream(d))
			{
			byte[] buf=new byte[blocSize];
			byte[] h;
			int s;
			while ((s=fis.read(buf))!=-1)
				{
				int c;
				while (s<buf.length&&(c=fis.read())!=-1)
					buf[s++]=(byte)c;
				while (s<buf.length)
					buf[s++]=0;
				fileMd.update(buf);
				h=md.digest(buf);
				md.reset();
				desc.getHash().add(new Hash(h));
				desc.getRoll().add(RollingChecksum.compute(buf));
				}
			h=fileMd.digest();
			desc.setFileHash(new Hash(h));
			}
		return desc;
		}

	public static void load(Map<String,FileDesc> files, String path, String f, int blocSize) throws IOException, NoSuchAlgorithmException
		{
		File d=new File(path, f);
		if(d.isDirectory())
			{
			String[] fl=d.list();
			for(int i=0; i<fl.length; i++)
				load(files, path, f+'/'+fl[i], blocSize);
			}
		else if(d.isFile())
			{
			FileDesc fd=load(f, d, blocSize);
			files.put(fd.getName().toString(), fd);
			}
		}

	public static void load(Set<FileDesc> files, String path, String f, int blocSize) throws IOException, NoSuchAlgorithmException
		{
		File d=new File(path, f);
		if(d.isDirectory())
			{
			String[] fl=d.list();
			for(int i=0; i<fl.length; i++)
				load(files, path, f+'/'+fl[i], blocSize);
			}
		else if(d.isFile())
			files.add(load(f, d, blocSize));
		}
	}
