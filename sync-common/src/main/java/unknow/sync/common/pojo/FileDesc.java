package unknow.sync.common.pojo;

import java.util.List;

import unknow.sync.proto.BlocInfo;

/**
 * hold off info of a file
 * 
 * @author unknow
 */
public class FileDesc {
	/** name */
	public final String name;
	/** bloc info */
	public final List<BlocInfo> blocs;
	/** hash of the file */
	public final long hash;
	/** size of the file */
	public final long size;

	/**
	 * create new FileDesc
	 * 
	 * @param name
	 * @param blocs
	 * @param hash
	 * @param size
	 */
	public FileDesc(String name, List<BlocInfo> blocs, long hash, long size) {
		super();
		this.name = name;
		this.blocs = blocs;
		this.hash = hash;
		this.size = size;
	}
}