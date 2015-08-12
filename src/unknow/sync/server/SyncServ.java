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
import unknow.sync.*;
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
		String login=null;
		String project=null;
		long lastUsed=-1;
		CharSequence appendFile;
		File tmp;
		RandomAccessFile orgFile=null;
		FileOutputStream fos=null;
		public FileInputStream fis;
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

	private static long lsb=0;
	private static long msb=0;
	private static Object mutex=new Object();

	public static UUID nextUUID()
		{
		byte[] b=new byte[16];
		long m;
		long l;
		synchronized (mutex)
			{
			l=lsb++;
			if(lsb==-1)
				msb++;
			m=msb;
			}

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

	private class SyncImpl implements Sync
		{
		public UUID login(CharSequence login, CharSequence pass) throws AvroRemoteException
			{
			String p=users.optString(login.toString(), null);
			if("anonymous".contentEquals(login)||p!=null&&p.equals(pass.toString()))
				{
				UUID random=nextUUID();
				while (states.containsKey(random))
					random=nextUUID();
				State s=new State();
				s.login=login.toString();
				s.lastUsed=System.currentTimeMillis();
				states.put(random, s);
				return random;
				}
			return null;
			}

		public ProjectInfo selectProject(UUID uuid, CharSequence project) throws AvroRemoteException
			{
			State s=states.get(uuid);
			String p=project.toString();
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			try
				{
				if(s==null||!projects.containsKey(p)||!projects.get(p).asRight(s.login, "read"))
					return null;
				s.project=p;
				Project proj=projects.get(s.project);
				return new ProjectInfo(proj.blocSize(), proj.fileDesc());
				}
			catch (JsonException e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public ByteBuffer getFile(UUID uuid, CharSequence file) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project))
				return null;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "read"))
					return null;
				File f=new File(project.path(), file.toString());
				if(f.isFile())
					{
					System.out.println("sending file "+file);
					byte[] buf=new byte[2048];
					s.fis=new FileInputStream(f);
					int l=s.fis.read(buf);
					return ByteBuffer.wrap(buf, 0, l);
					}
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			return null;
			}

		public ByteBuffer getNext(UUID uuid) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project)||s.fis==null)
				return null;
			try
				{
				byte[] buf=new byte[2048];
				int l=s.fis.read(buf);
				if(l==-1)
					{
					try
						{
						s.fis.close();
						}
					catch (IOException e)
						{
						}
					s.fis=null;
					return null;
					}
				return ByteBuffer.wrap(buf, 0, l);
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public ByteBuffer getBloc(UUID uuid, CharSequence file, int bloc) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project))
				return null;
			Project project=projects.get(s.project);
			RandomAccessFile ram=null;
			try
				{
				if(!project.asRight(s.login, "read"))
					return null;
				int blocSize=project.blocSize();
				File f=new File(project.path(), file.toString());
				if(f.isFile())
					{
					ram=new RandomAccessFile(f, "r");
					ram.seek(bloc*blocSize);
					byte[] buf=new byte[blocSize];
					int l;
					int c=0;
					while (c<blocSize&&(l=ram.read(buf, c, blocSize-c))>=0)
						c+=l;
					return ByteBuffer.wrap(buf);
					}
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
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
			return null;
			}

		public boolean putFile(UUID uuid, CharSequence file, Hash hash, ByteBuffer data) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project))
				return false;
			Project project=projects.get(s.project);
			FileOutputStream fos=null;
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
				File tmp=File.createTempFile("sync_", ".tmp");
				fos=new FileOutputStream(tmp);
				fos.write(data.array());
				return checkFile(project, file, tmp, hash);
				}
			catch (AvroRemoteException e)
				{
				log.error("", e);
				throw e;
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			finally
				{
				if(fos!=null)
					{
					try
						{
						fos.flush();
						fos.close();
						}
					catch (IOException e)
						{
						}
					}
				}
			}

		public boolean startAppend(UUID uuid, CharSequence file) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project))
				return false;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
				File f=new File(project.path(), file.toString());

				s.appendFile=file;
				if(f.exists())
					s.orgFile=new RandomAccessFile(f, "r");
				else
					f.getParentFile().mkdirs();
				s.tmp=File.createTempFile("sync_", ".tmp");
				s.fos=new FileOutputStream(s.tmp);
				return true;
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
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project)||s.fos==null)
				return false;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
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
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project)||s.orgFile==null||s.fos==null)
				return false;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
				int blocSize=project.blocSize();
				s.orgFile.seek(bloc*blocSize);
				int l=blocSize*(count==null?1:count);
				byte[] buf=new byte[blocSize];
				while (l>0)
					{
					int read=s.orgFile.read(buf);
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

		public Object endAppend(UUID uuid, Hash hash) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project)||s.fos==null)
				return false;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
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
				CharSequence file=s.appendFile;
				boolean ret=checkFile(project, file, s.tmp, hash);
				s.appendFile=null;
				s.orgFile=null;
				s.tmp=null;
				s.fos=null;
				return ret?ret:project.get(file.toString());
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		public boolean delete(UUID uuid, CharSequence file) throws AvroRemoteException
			{
			State s=states.get(uuid);
			if(s!=null)
				s.lastUsed=System.currentTimeMillis();
			if(s==null||!projects.containsKey(s.project))
				return false;
			Project project=projects.get(s.project);
			try
				{
				if(!project.asRight(s.login, "write"))
					return false;
				if(project.remove(file.toString()))
					{
					File f=new File(project.path(), file.toString());
					return f.delete();
					}
				return false;
				}
			catch (Exception e)
				{
				log.error("", e);
				throw new AvroRemoteException(e);
				}
			}

		private boolean checkFile(Project project, CharSequence fileName, File f, Hash expectedhash) throws AvroRemoteException
			{
			try
				{
				FileDesc desc=Common.load(fileName, f, project.blocSize());

				boolean ret=desc.getFileHash().equals(expectedhash);
				File org=new File(project.path(), fileName.toString());
				org.getParentFile().mkdirs();
				Files.move(Paths.get(f.getAbsolutePath()), Paths.get(org.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);

				project.add(desc); // replace fileDesc & file on disk
				return ret;
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
		//SyncServ syncServ=
		new SyncServ(cfg);

		}
	}