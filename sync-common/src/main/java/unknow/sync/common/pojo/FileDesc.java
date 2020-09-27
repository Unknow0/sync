package unknow.sync.common.pojo;

/**
 * hold off info of a file
 * 
 * @author unknow
 */
public class FileDesc {
	/** name */
	public String name;
	/** bloc info */
	public Bloc[] blocs;
	/** hash of the file */
	public long hash;
	/** size of the file */
	public long size;

	/**
	 * create new FileDesc
	 * 
	 * @param name
	 */
	public FileDesc(String name) {
		this.name = name;
	}
}