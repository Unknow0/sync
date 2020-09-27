/**
 * 
 */
package unknow.sync.client;

import java.io.IOException;
import java.net.Socket;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.sync.Query;
import unknow.sync.proto.pojo.ProjectInfo;

/**
 * @author unknow
 */
public class SyncConnection {
	private static final Logger log = LoggerFactory.getLogger(SyncConnection.class);
	private Socket socket;

	private MessagePacker pack;
	private MessageUnpacker unpack;

	/**
	 * create new SyncConnection
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public SyncConnection(String host, int port) throws IOException {
		socket = new Socket(host, port);
		unpack = MessagePack.newDefaultUnpacker(socket.getInputStream());
		pack = MessagePack.newDefaultPacker(socket.getOutputStream());
	}

	/**
	 * log to the server
	 * 
	 * @param token
	 * @return the projectInfo
	 * @throws IOException
	 */
	public ProjectInfo login(String token) throws IOException {
		pack.packInt(Query.LOGIN.ordinal());
		pack.packString(token);
		pack.flush();
		log.info("login");

		return new ProjectInfo(unpack);
	}

	/**
	 * get bloc of a file
	 * 
	 * @param file
	 * @return the file's bloc
	 * @throws IOException
	 */
	public BlocToProcess[] fileBlocs(String file) throws IOException {
		pack.packInt(Query.FILEBLOC.ordinal());
		pack.packString(file);
		pack.flush();

		int len = unpack.unpackArrayHeader();
		BlocToProcess[] b = new BlocToProcess[len];
		for (int i = 0; i < len; i++)
			b[i] = new BlocToProcess(unpack);
		return b;
	}

	/**
	 * download a bloc
	 * 
	 * @param file
	 * @param bloc
	 * @return the bloc's data
	 * @throws IOException
	 */
	public byte[] getBloc(String file, int bloc) throws IOException {
		pack.packInt(Query.GETBLOC.ordinal());
		pack.packString(file);
		pack.packInt(bloc);

		pack.flush();

		int len = unpack.unpackBinaryHeader();
		byte[] buf = new byte[len];
		unpack.readPayload(buf);

		return buf;
	}

	/**
	 * 
	 * @param file
	 * @param offset
	 * @param c
	 * @throws IOException
	 */
	public void getFile(String file, long offset, DataHandle c) throws IOException {
		log.info("	getFile({}, {})", file, offset);
		pack.packInt(Query.GETFILE.ordinal());
		pack.packString(file);
		pack.packLong(offset);
		pack.flush();

		while (!unpack.tryUnpackNil()) {
			int len = unpack.unpackBinaryHeader();
			byte[] buf = new byte[len];
			unpack.readPayload(buf);
			c.handle(buf);
		}
	}

	/**
	 * close
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		pack.close();
		unpack.close();
		socket.close();
	}

	/** handle data */
	public static interface DataHandle {
		/**
		 * data to handle
		 * 
		 * @param b the data to handle
		 * @throws IOException
		 */
		void handle(byte[] b) throws IOException;
	}
}
