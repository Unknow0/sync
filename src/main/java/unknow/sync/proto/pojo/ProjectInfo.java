package unknow.sync.proto.pojo;

public class ProjectInfo
	{
	public int blocSize;
	public FileHash[] hashs;

	public ProjectInfo()
		{
		}

	public ProjectInfo(java.lang.Integer blocSize, FileHash[] hashs)
		{
		this.blocSize=blocSize;
		this.hashs=hashs;
		}
	}
