package unknow.sync;

/**
 * 
 * @author Unknow
 */
public class RollingChecksum
	{
	private byte[] buf;
	private int off;
	private boolean filled;
	private int sum;
	private int decl;
	private int nDecl;

	public RollingChecksum(int size)
		{
		buf=new byte[size];
		off=0;
		filled=false;
		sum=0;
		decl=(size-1)%32;
		nDecl=32-decl;
		}

	public int append(byte b)
		{
		if(filled)
			{
			int lb=buf[off]&0xFF;
			sum=((lb<<decl)|(lb>>nDecl))^sum;
			}
		sum=((sum<<1)|(sum>>>31))^(b&0xFF);
		buf[off]=b;
		off=(off+1)%buf.length;
		if(!filled&&off==0)
			filled=true;
		return sum;
		}
	
	public int sum()
		{
		return sum;
		}

	public byte[] buf()
		{
		byte[] b=new byte[buf.length];
		int i=off, j=0;
		do
			{
			b[j++]=buf[i];
			i=(i+1)%buf.length;
			}
		while (i!=off);
		return b;
		}

	public static int compute(byte[] buf)
		{
		return compute(buf, 0, buf.length);
		}

	public static int compute(byte[] buf, int off, int len)
		{
		int sum=0;
		for(int i=off; i<off+len; i++)
			sum=((sum<<1)|(sum>>>31))^(buf[i]&0xFF);
		return sum;
		}
	}
