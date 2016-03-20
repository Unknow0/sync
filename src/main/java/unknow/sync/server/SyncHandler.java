package unknow.sync.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.slf4j.*;

import unknow.sync.*;
import unknow.sync.proto.*;
import unknow.sync.proto.pojo.*;
import unknow.sync.proto.pojo.UUID;

@Sharable
public class SyncHandler extends ChannelHandlerAdapter
	{
	private static final Logger log=LoggerFactory.getLogger(SyncHandler.class);
	private static final Done DONE=new Done();
	private SyncServ serv;

	public SyncHandler(SyncServ serv)
		{
		this.serv=serv;
		}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
		{
		Object res=null;
		try
			{
			if(msg instanceof LoginReq)
				{
				LoginReq l=(LoginReq)msg;
				res=login(l.login, l.pass, l.project, l.action);
				}
			else if(msg instanceof GetFileDescs)
				{
				GetFileDescs f=(GetFileDescs)msg;
				res=getFileDescs(f.uuid, f.fileId);
				}
			else if(msg instanceof GetBlocReq)
				{
				GetBlocReq r=(GetBlocReq)msg;
				res=getBloc(r.uuid, r.name, r.off, r.len);
				}
			else if(msg instanceof DeleteReq)
				{
				DeleteReq d=(DeleteReq)msg;
				res=delete(d.uuid, d.file);
				}
			else if(msg instanceof GetFileReq)
				{
				GetFileReq g=(GetFileReq)msg;
				res=getFile(g.uuid, g.name, g.off);
				}
			else if(msg instanceof StartAppend)
				{
				StartAppend a=(StartAppend)msg;
				startAppend(a.uuid, a.name);
				}
			else if(msg instanceof AppendData)
				{
				AppendData a=(AppendData)msg;
				appendData(a.uuid, a.data);
				}
			else if(msg instanceof AppendBloc)
				{
				AppendBloc a=(AppendBloc)msg;
				appendBloc(a.uuid, a.off, a.len);
				}
			else if(msg instanceof EndAppend)
				{
				EndAppend a=(EndAppend)msg;
				res=endAppend(a.uuid, a.hash);
				}
			}
		catch (SyncException e)
			{
			log.warn("", e);
			res=e.getMessage();
			}
		if(res!=null)
			ctx.writeAndFlush(res);
		}

	private static final String ANONYMOUS="anonymous";

	public LoginRes login(String login, String pass, String project, Action action) throws SyncException
		{
		String pass2=serv.getPass(login);
		if(!ANONYMOUS.equals(login)&&(pass2==null||!pass2.equals(pass)))
			throw new SyncException("login failed");

		Project p=serv.getProject(project);
		if(p==null)
			throw new SyncException("unknown project");
		if(!p.asRight(login, action))
			throw new SyncException("no right");

		State s=serv.nextState();
		s.action=action;
		s.project=p;
		return new LoginRes(s.uuid, p.projectInfo());
		}

	public FileDesc[] getFileDescs(UUID uuid, int[] fileId) throws SyncException
		{
		State s=serv.getState(uuid);
		if(s==null)
			throw new SyncException("invalide state");

		FileDesc[] descs=new FileDesc[fileId.length];

		for(int i=0; i<fileId.length; i++)
			descs[i]=s.project.fileDesc(fileId[i]);

		return descs;
		}

	public byte[] getBloc(UUID uuid, String file, int bloc, int count) throws SyncException
		{
		State s=serv.getState(uuid);
		if(s==null||s.action!=Action.read)
			throw new SyncException("invalide state");

		RandomAccessFile ram=null;
		try
			{
			int blocSize=s.project.blocSize();
			File f=new File(s.project.path(), file.toString());
			if(f.isFile())
				{
				ram=new RandomAccessFile(f, "r");
				ram.seek(bloc*blocSize);
				int toRead=blocSize*count;
				byte[] buf=new byte[toRead];
				int l;
				int c=0;
				while (c<toRead&&(l=ram.read(buf, c, toRead-c))>=0)
					c+=l;

				return c==buf.length?buf:Arrays.copyOf(buf, c);
				}
			throw new SyncException("invalid file '"+file+"'");
			}
		catch (Exception e)
			{
			throw new SyncException(e);
			}
		finally
			{
			if(ram!=null)
				try
					{
					ram.close();
					}
				catch (IOException e)
					{
					}
			}
		}

	public Object getFile(UUID uuid, String file, long offset) throws SyncException
		{
		State s=serv.getState(uuid);
		if(s==null||s.action!=Action.read)
			throw new SyncException("invalide state");
		try
			{
			File f=new File(s.project.path(), file);
			if(f.isFile())
				{
				try (RandomAccessFile ram=new RandomAccessFile(f, "r"))
					{
					byte[] buf=new byte[131072]; // send bloc of 128K
					ram.seek(offset);
					int l=ram.read(buf);
					if(l<0)
						return DONE;
					return l==buf.length?buf:Arrays.copyOf(buf, l);
					}
				}
			throw new SyncException("invalid file");
			}
		catch (Exception e)
			{
			throw new SyncException(e);
			}
		}

	public void startAppend(UUID uuid, String file) throws SyncException
		{
		State s=serv.getState(uuid);
		if(s==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		try
			{
			File f=new File(s.project.path(), file.toString());

			s.appendFile=file;
			if(f.exists())
				s.orgFile=new RandomAccessFile(f, "r");
			else
				f.getParentFile().mkdirs();
			s.tmp=File.createTempFile("sync_", ".tmp");
			s.fos=new FileOutputStream(s.tmp);
			}
		catch (Exception e)
			{
			s.appendFile=null;
			if(s.orgFile!=null)
				try
					{
					s.orgFile.close();
					}
				catch (IOException e2)
					{
					}
			s.orgFile=null;
			if(s.tmp!=null)
				s.tmp.delete();
			s.tmp=null;
			if(s.fos!=null)
				try
					{
					s.fos.close();
					}
				catch (IOException e1)
					{
					}
			s.fos=null;
			throw new SyncException(e);
			}
		}

	public void appendData(UUID uuid, byte[] data) throws SyncException
		{
		State s=serv.getState(uuid);
		if(s==null||s.fos==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		try
			{
			s.fos.write(data);
			return;
			}
		catch (Exception e)
			{
			throw new SyncException(e);
			}
		}

	public void appendBloc(UUID uuid, int bloc, Integer count) throws SyncException
		{
		State s=serv.getState(uuid);

		if(s==null||s.fos==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		try
			{
			int blocSize=s.project.blocSize();
			s.orgFile.seek(bloc*blocSize);
			int l=blocSize*(count==null?1:count);
			byte[] buf=new byte[blocSize];
			while (l>0)
				{
				int read=s.orgFile.read(buf);
				if(read==-1)
					break;
				s.fos.write(buf, 0, read);
				l-=read;
				}
			return;
			}
		catch (Exception e)
			{
			throw new SyncException(e);
			}
		}

	public Object endAppend(UUID uuid, Hash hash) throws SyncException
		{
		State s=serv.getState(uuid);

		if(s==null||s.fos==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		try
			{
			if(s.orgFile!=null)
				{
				try
					{
					s.orgFile.close();
					}
				catch (IOException e)
					{
					log.warn("failed to close origin file", e);
					}
				}
			try
				{
				s.fos.close();
				}
			catch (IOException e)
				{
				log.warn("failed to close tmpfile file", e);
				}
			String file=s.appendFile;
			Files.move(s.tmp.toPath(), Paths.get(s.project.path(), file), StandardCopyOption.REPLACE_EXISTING);
			FileDesc fd=s.project.reloadFile(file);
			s.appendFile=null;
			s.orgFile=null;
			s.tmp=null;
			s.fos=null;
			return fd.fileHash.equals(hash)?DONE:fd;
			}
		catch (Exception e)
			{
			throw new SyncException(e);
			}
		}

	public boolean delete(UUID uuid, String file) throws SyncException
		{
		State s=serv.getState(uuid);

		if(s==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		return s.project.delete(file);
		}
	}
