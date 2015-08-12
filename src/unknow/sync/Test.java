package unknow.sync;

import java.io.*;

public class Test
	{
	public static void main(String[] arg) throws Exception
		{
		FileInputStream fis=new FileInputStream("target/lib/commons-compress-1.4.1.jar");
		RollingChecksum rcs=new RollingChecksum(256);
		
		int l=0;
		boolean done=false;
		while(!done)
			{
			for(int i=0; i<256; i++)
				{
				int b=fis.read();
				if(b<0)
					{
					for(; i<256; i++)
						rcs.append((byte)0);
					done=true;
					break;
					}
				rcs.append((byte)b);
				}
			if(RollingChecksum.compute(rcs.buf())!=rcs.sum())
				System.out.println(l+": failed");
			l++;
			}
		}
	}
