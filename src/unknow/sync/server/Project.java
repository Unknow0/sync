package unknow.sync.server;

import java.io.*;
import java.security.*;
import java.util.*;

import unknow.json.*;
import unknow.sync.proto.*;

public class Project
	{
	private Map<String,FileDesc> files;
	private JsonObject cfg;
	private String name;
	private int blocSize;

	public Project(String prj, JsonObject c) throws JsonException, NoSuchAlgorithmException, IOException
		{
		cfg=c;
		name=prj;
		File d=new File(cfg.getString("path"));
		if(d.isDirectory())
			{
			files=new HashMap<String,FileDesc>(10);
			blocSize=cfg.getInt("bloc_size");
			unknow.sync.Common.load(files, d.getPath(), "", blocSize);
			}
		else
			throw new FileNotFoundException("project '"+name+"' path isn't a directory");
		}

	public boolean asRight(String login, String action) throws JsonException
		{
		JsonArray right=cfg.optJsonArray(action);
		if(right!=null&&right.contains(login))
			return true;

		return "all".equals(cfg.optString(action, ""));
		}

	public int blocSize()
		{
		return blocSize;
		}

	public String path() throws JsonException
		{
		return cfg.getString("path");
		}

	public void add(FileDesc desc)
		{
		files.put(desc.getName().toString(), desc);
		}

	public FileDesc get(String file)
		{
		return files.get(file);
		}

	public boolean remove(String file)
		{
		return files.remove(file)!=null;
		}

	public List<FileDesc> fileDesc()
		{
		return new ArrayList<FileDesc>(files.values());
		}
	}
