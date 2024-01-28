package unknow.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.ByteString;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerBuilder;
import unknow.server.protobuf.ProtoStuffConnection;
import unknow.sync.common.pojo.FileDesc;
import unknow.sync.proto.GetBloc;
import unknow.sync.proto.GetFile;
import unknow.sync.proto.SyncMessage;

/**
 * 
 * @author Unknow
 */
public class SyncServ extends NIOServerBuilder {
	private static final Logger log = LoggerFactory.getLogger(SyncServ.class);

	private static final ExecutorService POOL = Executors.newCachedThreadPool();

	private Opt addr;
	private Opt path;
	private Opt bs;
	private Opt tokens;

	@Override
	protected void beforeParse() {
		addr = withOpt("addr").withCli(Option.builder("a").longOpt("addr").argName("port").desc("address to bind to").build()).withValue("54320");
		path = withOpt("path").withCli(Option.builder("p").longOpt("path").hasArg().argName("path").desc("folder with data to sync").build()).withValue(".");
		bs = withOpt("bs").withCli(Option.builder("b").longOpt("bs").argName("size").desc("block size to use").build()).withValue("1024");
		tokens = withOpt("tokens").withCli(Option.builder().longOpt("tokens").hasArg().desc("read only tokens").build()).withValue("anon");
	}

	@Override
	protected void process(NIOServer server, CommandLine cli) throws Exception {
		Project p = new Project(path.value(cli), parseInt(cli, bs, 0), new HashSet<>(Arrays.asList(tokens.value(cli).split(","))));
		server.bind(parseAddr(cli, addr, ""), () -> new ProtoCo(p));
	}

	public static void main(String[] arg) throws Exception {
		NIOServer server = new SyncServ().build(arg);

		server.start();
		server.await();
	}

	private static class ProtoCo extends ProtoStuffConnection<SyncMessage> {
		private final Project project;
		private final byte[] buf;

		private boolean logged = false;

		public ProtoCo(Project p) {
			super(SyncMessage.getSchema(), false);
			this.project = p;
			this.buf = new byte[project.blocSize()];
		}

		@Override
		protected void onInit() {
			super.onInit();
			logged = false;
		}

		@Override
		protected void process(SyncMessage t) throws IOException {
			if (t.getToken() != null)
				login(t.getToken());

			if (!logged) {
				getOut().close();
				return;
			}
			if (t.getInfo() != null)
				getInfo(t.getInfo());
			if (t.getFile() != null)
				getFile(t.getFile());
			if (t.getBloc() != null)
				getBloc(t.getBloc());
		}

		private void login(String token) throws IOException {
			log.trace("	login({})", token);
			if (project.asRight(token)) {
				logged = true;
				writeMessage(project.projectInfo());
			}
		}

		private void getInfo(String file) throws IOException {
			log.trace("	getInfo({})", file);

			FileDesc fileDesc = project.fileDesc(file);
			if (fileDesc == null) {
				out.close();
				return;
			}

			SyncMessage m = new SyncMessage();
			m.setBlocsList(fileDesc.blocs);
			write(m);
		}

		private void getBloc(GetBloc bloc) throws IOException {
			String file = bloc.getFile();
			int off = bloc.getBloc();
			log.trace("	getBloc({}, {})", file, off);

			Path f = project.file(file);
			if (f == null) {
				out.close();
				return;
			}

			try (InputStream in = Files.newInputStream(f)) {
				int len = project.blocSize();
				in.skip(off * len);
				int o = 0;
				do {
					int count = in.read(buf, o, len);
					if (count < 0)
						break;
					len -= count;
					o += count;
				} while (len > 0);
				SyncMessage m = new SyncMessage();
				m.setData(ByteString.copyFrom(buf, 0, o));
				write(m);
			}
		}

		private void getFile(GetFile getfile) throws IOException {
			String file = getfile.getFile();
			long off = getfile.getOffset();
			log.trace("getFile	({}, {})", file, off);

			Path f = project.file(file);
			if (f == null) {
				out.close();
				return;
			}

			POOL.submit(new SendFile(out, f, off));
		}
	}
}