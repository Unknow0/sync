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

import com.google.protobuf.ByteString;

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

	/**
	 * log to the server
	 * 
	 * @param token
	 * @return the projectInfo
	 * @throws IOException
	 */
	public ProjectInfo login(String token) throws IOException {
		SyncMessage.newBuilder().setToken(token).build().writeDelimitedTo(out);
		out.flush();
		log.info("login");

		SyncMessage.Builder b = SyncMessage.newBuilder();
		if (!b.mergeDelimitedFrom(in) || !b.hasProject())
			throw new IOException("wrong response");
		return b.getProject();
	}

	/**
	 * get bloc of a file
	 * 
	 * @param file
	 * @return the file's bloc
	 * @throws IOException
	 */
	public List<BlocInfo> fileBlocs(String file) throws IOException {
		SyncMessage.newBuilder().setFile(file).build().writeDelimitedTo(out);
		out.flush();

		SyncMessage.Builder b = SyncMessage.newBuilder();
		if (!b.mergeDelimitedFrom(in) || !b.hasBloc())
			throw new IOException("wrong response");
		return b.getBloc().getBlocsList();
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
		SyncMessage.newBuilder().setGetbloc(GetBloc.newBuilder().setFile(file).setBloc(bloc)).build().writeDelimitedTo(out);
		out.flush();

		SyncMessage.Builder b = SyncMessage.newBuilder();
		if (!b.mergeDelimitedFrom(in) || !b.hasData())
			throw new IOException("wrong response");
		return b.getData();
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
		SyncMessage.newBuilder().setGetfile(GetFile.newBuilder().setFile(file).setOffset(offset)).build().writeDelimitedTo(out);
		out.flush();

		SyncMessage.Builder b = SyncMessage.newBuilder();
		while (true) {
			if (!b.mergeDelimitedFrom(in) || !b.hasData())
				throw new IOException("wrong response");
			if (b.getData().size() == 0)
				return;
			b.getData().writeTo(c);
			b.clearData();
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
