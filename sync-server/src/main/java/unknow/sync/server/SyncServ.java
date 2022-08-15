package unknow.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import unknow.server.nio.Connection;
import unknow.server.nio.Connection.Out;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;
import unknow.server.protobuf.ProtobufHandler;
import unknow.sync.common.pojo.FileDesc;
import unknow.sync.proto.BlocList;
import unknow.sync.proto.GetBloc;
import unknow.sync.proto.GetFile;
import unknow.sync.proto.SyncMessage;

/**
 * 
 * @author Unknow
 */
public class SyncServ extends NIOServerCli {
	private static final Logger log = LoggerFactory.getLogger(SyncServ.class);

	private static final ExecutorService POOL = Executors.newCachedThreadPool();

	/** data path */
	@Option(names = { "--data", "-d" }, required = true, description = "Set the root of all data")
	public String path;

	/** bloc size */
	@Option(names = { "--bloc-size", "-bs" }, required = true, description = "Set the size of the bloc")
	public int blocSize;

	/** allowed token */
	@Option(names = "--tokens", description = "Set read only token")
	public Set<String> tokens = new HashSet<>();

	private Project project;

	@Override
	protected void init() {
		try {
			project = new Project(path, blocSize, tokens);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		handler = new HandlerFactory() {

			@Override
			public unknow.server.nio.Handler create(Connection c) {
				return new Handler(c);
			}
		};
	}

	public static void main(String[] arg) {
		System.exit(new CommandLine(new SyncServ()).execute(arg));
	}

	private class Handler extends ProtobufHandler<SyncMessage> {
		private boolean logged = false;
		private final byte[] buf = new byte[4096];

		private Handler(Connection c) {
			super(SyncMessage.parser(), c);
		}

		@Override
		protected void process(SyncMessage t) throws IOException {
			Out out = co.getOut();
			if (t.hasToken()) {
				String token = t.getToken();
				log.trace("	({}, {})", token);

				if (!project.asRight(token)) {
					out.close();
					return;
				}
				logged = true;
				project.projectInfo().writeDelimitedTo(out);
				out.flush();
				return;
			}
			if (!logged) {
				out.close();
				return;
			}
			if (t.hasGetfile()) {
				GetFile getfile = t.getGetfile();
				String file = getfile.getFile();
				long off = getfile.getOffset();
				log.trace("	({}, {})", file, off);

				Path f = project.file(file);
				if (f == null) {
					out.close();
					return;
				}

				POOL.submit(new SendFile(out, f, off));
			} else if (t.hasFile()) {
				String file = t.getFile();
				log.trace("	({})", file);

				FileDesc fileDesc = project.fileDesc(file);
				if (fileDesc == null) {
					out.close();
					return;
				}

				SyncMessage.newBuilder().setBloc(BlocList.newBuilder().addAllBlocs(fileDesc.blocs)).build().writeDelimitedTo(out);
				out.flush();
			} else if (t.hasGetbloc()) {
				GetBloc getbloc = t.getGetbloc();
				String file = getbloc.getFile();
				int off = getbloc.getBloc();
				log.trace("	({}, {})", file, off);

				Path f = project.file(file);
				if (f == null) {
					out.close();
					return;
				}

				try (InputStream in = Files.newInputStream(f)) {
					int len = project.blocSize();
					in.skip(off * len);
					Output b = ByteString.newOutput(len);
					do {
						int count = in.read(buf, 0, len);
						if (count < 0)
							break;
						b.write(buf, 0, count);
						len -= count;
					} while (len > 0);
					SyncMessage.newBuilder().setData(b.toByteString()).build().writeDelimitedTo(out);
					out.flush();
				}
			}
		}
	}
}