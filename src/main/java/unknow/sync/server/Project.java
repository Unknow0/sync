package unknow.sync.server;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import org.slf4j.*;

import unknow.json.*;
import unknow.sync.*;
import unknow.sync.proto.pojo.*;

public class Project
	{
	private static final Logger log=LoggerFactory.getLogger(Project.class);
	private List<FileDesc> files;
	private JsonObject cfg;
	private String name;
	private Path root;

	private ProjectInfo projectInfo;

	public Project(String prj, JsonObject c) throws JsonException, NoSuchAlgorithmException, IOException
		{
		cfg=c;
		name=prj;
		root=Paths.get(cfg.getString("path"));

		if(!Files.exists(root))
			{
			log.warn("root directory of '{}' not found creating '{}'", name, root);
			Files.createDirectories(root);
			}
		if(Files.isDirectory(root))
			{
			files=new ArrayList<FileDesc>();
			projectInfo=new ProjectInfo(cfg.getInt("bloc_size"), null);
			FileDescLoader.load(files, root, projectInfo.blocSize, null);

			projectInfo.hashs=new FileHash[files.size()];
			int i=0;
			for(FileDesc fd:files)
				projectInfo.hashs[i++]=new FileHash(fd.name, fd.fileHash);
			}
		else
			throw new FileNotFoundException("project '"+name+"' path isn't a directory");
		}

	public boolean asRight(String login, Action action)
		{
		try
			{
			JsonArray right=cfg.optJsonArray(action.toString());
			if(right!=null&&right.contains(login))
				return true;

			return "all".equals(cfg.optString(action.toString(), ""));
			}
		catch (Exception e)
			{
			log.error("failed to check right on '{}' for '{}'", name, login);
			}
		return false;
		}

	public int blocSize()
		{
		return projectInfo.blocSize;
		}

	public String path()
		{
		return cfg.optString("path");
		}

	public FileDesc fileDesc(int i)
		{
		synchronized (this)
			{
			return files.get(i);
			}
		}

	public List<FileDesc> fileDesc()
		{
		synchronized (this) // TODO change to readWriteLock
			{
			return files;
			}
		}

	public ProjectInfo projectInfo()
		{
		synchronized (this)
			{
			return projectInfo;
			}
		}

	public FileDesc reloadFile(String file) throws NoSuchAlgorithmException, FileNotFoundException, IOException
		{
		FileDesc fd=FileDescLoader.loadFile(root, Paths.get(file), projectInfo.blocSize);
		synchronized (this)
			{
			for(int i=0; i<projectInfo.hashs.length; i++)
				{
				if(fd.name.equals(projectInfo.hashs[i].name))
					{
					files.set(i, fd);
					projectInfo.hashs[i]=new FileHash(fd.name, fd.fileHash);
					return fd;
					}
				}
			projectInfo.hashs=Arrays.copyOf(projectInfo.hashs, projectInfo.hashs.length+1);
			projectInfo.hashs[projectInfo.hashs.length-1]=new FileHash(fd.name, fd.fileHash);
			files.add(fd);
			}
		return fd;
		}

	public boolean delete(String file)
		{
		synchronized (this)
			{
			Iterator<FileDesc> it1=files.iterator();
			int i=0;
			while (it1.hasNext())
				{
				it1.next();
				FileHash next=projectInfo.hashs[i++];
				if(next.name.equals(file))
					{
					try
						{
						Files.delete(root.resolve(file));

						it1.remove();
						projectInfo.hashs[i-1]=projectInfo.hashs[projectInfo.hashs.length-1];
						projectInfo.hashs=Arrays.copyOf(projectInfo.hashs, projectInfo.hashs.length-1);
						return true;
						}
					catch (IOException e)
						{
						log.warn("failed to delete '"+file+"'", e);
						return false;
						}
					}
				}
			}

		return false;
		}
	}