/**
 * 
 */
package unknow.sync.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.ByteString;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import unknow.sync.proto.BlocInfo;
import unknow.sync.proto.GetBloc;
import unknow.sync.proto.GetFile;
import unknow.sync.proto.ProjectInfo;
import unknow.sync.proto.SyncMessage;

/**
 * @author unknow
 */
public class SyncConnection {
	private static final Logger log = LoggerFactory.getLogger(SyncConnection.class);

	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;

	/**
	 * create new SyncConnection
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public SyncConnection(String host, int port) throws IOException {
		socket = new Socket(host, port);
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}

	private void write(SyncMessage m) throws IOException {
		LinkedBuffer buffer = LinkedBuffer.allocate();
		ProtobufIOUtil.writeDelimitedTo(out, m, SyncMessage.getSchema(), buffer);
		out.flush();
	}

	/**
	 * log to the server
	 * 
	 * @param token
	 * @return the projectInfo
	 * @throws IOException
	 */
	public ProjectInfo login(String token) throws IOException {
		SyncMessage m = new SyncMessage();
		m.setToken(token);
		write(m);
		log.info("login");

		ProtobufIOUtil.mergeDelimitedFrom(in, m, SyncMessage.getSchema());
		if (m.getProject() == null)
			throw new IOException("wrong response");
		return m.getProject();
	}

	/**
	 * get bloc of a file
	 * 
	 * @param file
	 * @return the file's bloc
	 * @throws IOException
	 */
	public List<BlocInfo> fileBlocs(String file) throws IOException {
		SyncMessage m = new SyncMessage();
		m.setInfo(file);
		write(m);

		ProtobufIOUtil.mergeDelimitedFrom(in, m, SyncMessage.getSchema());
		if (m.getBlocsList() == null)
			throw new IOException("wrong response");
		return m.getBlocsList();
	}

	/**
	 * download a bloc
	 * 
	 * @param file
	 * @param bloc
	 * @return the bloc's data
	 * @throws IOException
	 */
	public ByteString getBloc(String file, int bloc) throws IOException {
		SyncMessage m = new SyncMessage();
		m.setBloc(new GetBloc(file, bloc));
		write(m);

		ProtobufIOUtil.mergeDelimitedFrom(in, m, SyncMessage.getSchema());
		if (m.getData() == null)
			throw new IOException("wrong response");
		return m.getData();
	}

	/**
	 * 
	 * @param file
	 * @param offset
	 * @param c
	 * @throws IOException
	 */
	public void getFile(String file, long offset, OutputStream c) throws IOException {
		log.info("	getFile({}, {})", file, offset);
		SyncMessage m = new SyncMessage();
		m.setFile(new GetFile(file, offset));
		write(m);

		while (true) {
			ProtobufIOUtil.mergeDelimitedFrom(in, m, SyncMessage.getSchema());
			if (m.getData() == null)
				throw new IOException("wrong response");
			if (m.getData().size() == 0)
				return;
			ByteString.writeTo(c, m.getData());
			m.setData(null);
		}
	}

	/**
	 * close
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		in.close();
		out.close();
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
		void handle(ByteString b) throws IOException;
	}
}
