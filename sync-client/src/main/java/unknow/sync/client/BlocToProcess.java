/**
 * 
 */
package unknow.sync.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.core.MessageUnpacker;

import unknow.sync.common.pojo.Bloc;

/**
 * a bloc to process
 * 
 * @author unknow
 */
public class BlocToProcess extends Bloc {
	/** offset of found bloc */
	public List<Long> found = new ArrayList<>();

	/**
	 * unserialize a Bloc
	 * 
	 * @param u the unpacker
	 * @throws IOException
	 */
	public BlocToProcess(MessageUnpacker u) throws IOException {
		super(u);
	}

}
