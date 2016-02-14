package unknow.sync.server;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import org.apache.logging.log4j.*;

import unknow.json.*;
import unknow.sync.*;
import unknow.sync.proto.*;

public class Project
	{
	private static final Logger log=LogManager.getFormatterLogger(Project.class);
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
			log.warn("root directory of '%s' not found creating '%s'", name, root);
			Files.createDirectories(root);
			}
		if(Files.isDirectory(root))
			{
			files=new ArrayList<FileDesc>();
			projectInfo=new ProjectInfo(cfg.getInt("bloc_size"), new ArrayList<FileHash>());
			FileDescLoader.load(files, root, projectInfo.getBlocSize(), null);
			for(FileDesc fd:files)
				{
				projectInfo.getHashs().add(new FileHash(fd.getName(), fd.getFileHash()));
				}
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
			log.error("failed to check right on '%' for '%s'", name, login);
			}
		return false;
		}

	public int blocSize()
		{
		return projectInfo.getBlocSize();
		}

	public String path() throws JsonException
		{
		return cfg.getString("path");
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
		synchronized (this) // change to readWriteLock
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
		FileDesc fd=FileDescLoader.loadFile(root, Paths.get(file), projectInfo.getBlocSize());
		synchronized (this)
			{
			files.add(fd);
			projectInfo.getHashs().add(new FileHash(fd.getName(), fd.getFileHash()));
			}
		return fd;
		}

	public boolean delete(String file)
		{
		synchronized (this)
			{
			Iterator<FileDesc> it1=files.iterator();
			Iterator<FileHash> it2=projectInfo.getHashs().iterator();
			while (it1.hasNext())
				{
				it1.next();
				FileHash next=it2.next();
				if(next.getName().equals(file))
					{
					try
						{
						Files.delete(root.resolve(file));

						it1.remove();
						it2.remove();
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