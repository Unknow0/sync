package unknow.sync.server;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.*;
import io.netty.channel.socket.*;
import io.netty.channel.socket.nio.*;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import org.slf4j.*;

import unknow.common.*;
import unknow.common.kryo.*;
import unknow.json.*;
import unknow.sync.*;
import unknow.sync.proto.*;
import unknow.sync.proto.pojo.*;
import unknow.sync.proto.pojo.UUID;

/**
 * 
 * @author Unknow
 */
@Sharable
public class SyncServ extends ChannelHandlerAdapter
	{
	private static final Logger log=LoggerFactory.getLogger(SyncServ.class);
	private static final Done DONE=new Done();
	private Cfg cfg;

	private Map<String,Project> projects;
	private JsonObject users;

	private Map<UUID,State> states;

	private Kryos kryos;

	private static class State
		{
		Project project=null;
		String appendFile;
		File tmp;
		RandomAccessFile orgFile=null;
		FileOutputStream fos=null;
		UUID uuid;
		Action action;
		}

	public SyncServ(Cfg c) throws JsonException, IOException, NoSuchAlgorithmException
		{
		cfg=c;
		projects=new HashMap<String,Project>();
		states=new HashMap<>();
		users=cfg.getJsonObject("server.sync.users");
		log.info("load");
		loadProjects();

		kryos=new Kryos();
		log.info("ready");
		}

	/**
	 * reload a clean repertory
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws JsonException
	 */
	public void reload() throws IOException, NoSuchAlgorithmException, JsonException
		{
		projects.clear();
		loadProjects();
		}

	private void loadProjects() throws NoSuchAlgorithmException, IOException, JsonException
		{
		for(String prj:cfg.getJsonObject("server.sync.projects"))
			loadProject(prj);
		}

	private void loadProject(String prj) throws NoSuchAlgorithmException, IOException, JsonException
		{
		JsonObject o=cfg.getJsonObject("server.sync.projects."+prj);
		Project p=new Project(prj, o);
		projects.put(prj, p);
		}

	private long lsb=0;
	private long msb=0;
	private Object mutex=new Object();

	public State nextState()
		{
		State s=new State();
		UUID uuid;
		synchronized (mutex)
			{
			uuid=nextUUID();
			while (states.containsKey(uuid))
				uuid=nextUUID();
			states.put(uuid, s);
			}
		s.uuid=uuid;
		return s;
		}

	private UUID nextUUID()
		{
		byte[] b=new byte[16];
		long m;
		long l;
		l=lsb++;
		if(lsb==-1)
			msb++;
		m=msb;

		b[0]=(byte)((m>>56)&0xFF);
		b[1]=(byte)((m>>48)&0xFF);
		b[2]=(byte)((m>>40)&0xFF);
		b[3]=(byte)((m>>32)&0xFF);
		b[4]=(byte)((m>>24)&0xFF);
		b[5]=(byte)((m>>16)&0xFF);
		b[6]=(byte)((m>>8)&0xFF);
		b[7]=(byte)((m)&0xFF);
		b[8]=(byte)((l>>56)&0xFF);
		b[9]=(byte)((l>>48)&0xFF);
		b[10]=(byte)((l>>40)&0xFF);
		b[11]=(byte)((l>>32)&0xFF);
		b[12]=(byte)((l>>24)&0xFF);
		b[13]=(byte)((l>>16)&0xFF);
		b[14]=(byte)((l>>8)&0xFF);
		b[15]=(byte)((l)&0xFF);

		return new UUID(b);
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
		String pass2=users.optString(login);
		if(!ANONYMOUS.equals(login)&&(pass2==null||!pass2.equals(pass)))
			throw new SyncException("login failed");

		Project p=projects.get(project);
		if(p==null)
			throw new SyncException("unknown project");
		if(!p.asRight(login, action))
			throw new SyncException("no right");

		State s=nextState();
		s.action=action;
		s.project=p;
		return new LoginRes(s.uuid, p.projectInfo());
		}

	public FileDesc[] getFileDescs(UUID uuid, int[] fileId) throws SyncException
		{
		State s=states.get(uuid);
		if(s==null)
			throw new SyncException("invalide state");

		FileDesc[] descs=new FileDesc[fileId.length];

		for(int i=0; i<fileId.length; i++)
			descs[i]=s.project.fileDesc(fileId[i]);

		return descs;
		}

	public byte[] getBloc(UUID uuid, String file, int bloc, int count) throws SyncException
		{
		State s=states.get(uuid);
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
		State s=states.get(uuid);
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
		State s=states.get(uuid);
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
		State s=states.get(uuid);
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
		State s=states.get(uuid);

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
		State s=states.get(uuid);

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
		State s=states.get(uuid);

		if(s==null||s.action!=Action.write)
			throw new SyncException("invalide state");
		return s.project.delete(file);
		}

	public static void main(String arg[]) throws JsonException, NoSuchAlgorithmException, IOException, InterruptedException
		{
		Cfg cfg=Cfg.getSystem();
		if(arg.length!=0)
			cfg=new Cfg(arg[0]);
		System.out.println(cfg);
		final SyncServ serv=new SyncServ(cfg);

		EventLoopGroup bossGroup=new NioEventLoopGroup();
		EventLoopGroup workerGroup=new NioEventLoopGroup();
		try
			{
			ServerBootstrap b=new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>()
				{ // (4)
					@Override
					public void initChannel(SocketChannel ch) throws Exception
						{
						ch.pipeline().addLast(new KryoDecoder(serv.kryos), new KryoEncoder(serv.kryos), serv);
						}
				}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

			// Bind and start to accept incoming connections.
			ChannelFuture f=b.bind(cfg.getInt("server.sync.port")).sync(); // TODO

			f.channel().closeFuture().sync();
			}
		finally
			{
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			}
		}
	}