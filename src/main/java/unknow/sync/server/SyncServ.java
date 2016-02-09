package unknow.sync.server;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import org.apache.avro.*;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.logging.log4j.*;

import unknow.common.*;
import unknow.json.*;
import unknow.sync.proto.*;
import unknow.sync.proto.UUID;

/**
 * 
 * @author Unknow
 */
public class SyncServ
	{
	private static final Logger log=LogManager.getFormatterLogger(SyncServ.class);
	private Cfg cfg;

	private NettyServer server;

	private Map<String,Project> projects;
	private JsonObject users;

	private Map<UUID,State> states;

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
		server=new NettyServer(new SpecificResponder(Sync.class, new SyncImpl()), new InetSocketAddress(cfg.getInt("server.sync.port")));
		server.start();
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

	private static final String ANONYMOUS="anonymous";

	private void exception(String desc) throws SyncException
		{
		SyncException e=new SyncException();
		e.setDesc(desc);
		log.error(desc, e);
		throw e;
		}

	private void exception(Exception cause) throws SyncException
		{
		SyncException e=new SyncException();
		e.setDesc("Unexpected Error");
		log.error("", cause);
		throw e;
		}

	private class SyncImpl implements Sync
		{
		@Override
		public LoginRes login(String login, String pass, String project, Action action) throws SyncException
			{
			String pass2=users.optString(login);
			if(!ANONYMOUS.equals(login)&&(pass2==null||!pass2.equals(pass)))
				exception("login failed");

			Project p=projects.get(project);
			if(p==null)
				exception("unknown project");
			if(!p.asRight(login, action))
				exception("no right");

			State s=nextState();
			s.action=action;
			s.project=p;
			return new LoginRes(s.uuid, p.projectInfo());
			}

		@Override
		public List<FileDesc> getFileDescs(UUID uuid, List<Integer> fileId) throws SyncException
			{
			State s=states.get(uuid);
			if(s==null)
				exception("invalide state");

			List<FileDesc> descs=new ArrayList<FileDesc>(fileId.size());

			for(Integer i:fileId)
				descs.add(s.project.fileDesc(i));

			return descs;
			}

		public ByteBuffer getBloc(UUID uuid, String file, int bloc, int count) throws SyncException
			{
			State s=states.get(uuid);
			if(s==null||s.action!=Action.read)
				exception("invalide state");

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

					return ByteBuffer.wrap(buf);
					}
				exception("invalid file '"+file+"'");
				}
			catch (Exception e)
				{
				exception(e);
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
			return null; // How can i get here???
			}

		public ByteBuffer getFile(UUID uuid, String file, long offset) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s==null||s.action!=Action.read)
				exception("invalide state");
			try
				{
				File f=new File(s.project.path(), file);
				if(f.isFile())
					{
					System.out.println("sending file "+file);
					try (RandomAccessFile ram=new RandomAccessFile(f, "r"))
						{
						byte[] buf=new byte[131072]; // send bloc of 128K
						ram.seek(offset);
						int l=ram.read(buf);
						if(l<0)
							return null;
						return ByteBuffer.wrap(buf, 0, l);
						}
					}
				exception("invalid file");
				}
			catch (Exception e)
				{
				exception(e);
				}
			return null; // How can i get here???
			}

		public Void startAppend(UUID uuid, String file) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s==null||s.action!=Action.write)
				exception("invalide state");
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
				return null;
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
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public boolean appendData(UUID uuid, ByteBuffer data) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s==null||s.fos==null||s.action!=Action.write)
				exception("invalide state");
			try
				{
				s.fos.write(data.array());
				return true;
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public boolean appendBloc(UUID uuid, int bloc, Integer count) throws AvroRemoteException
			{
			State s=states.get(uuid);

			if(s==null||s.fos==null||s.action!=Action.write)
				exception("invalide state");
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
				return true;
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public FileDesc endAppend(UUID uuid, Hash hash) throws AvroRemoteException
			{
			State s=states.get(uuid);

			if(s==null||s.fos==null||s.action!=Action.write)
				exception("invalide state");
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
				return fd.getFileHash().equals(hash)?null:fd;
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public boolean delete(UUID uuid, String file) throws AvroRemoteException
			{
			State s=states.get(uuid);

			if(s==null||s.action!=Action.write)
				exception("invalide state");
			try
				{
				return s.project.delete(file);
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}
		}

	public static void main(String arg[]) throws JsonException, NoSuchAlgorithmException, IOException
		{
		Cfg cfg=Cfg.getSystem();
		if(arg.length!=0)
			cfg=new Cfg(arg[0]);
		System.out.println(cfg);
		new SyncServ(cfg);
		}
	}