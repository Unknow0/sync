package unknow.sync.server;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import unknow.sync.KryoDecoder;
import unknow.sync.KryoEncoder;
import unknow.sync.Kryos;
import unknow.sync.proto.pojo.UUID;
import unknow.sync.server.Cfg.P;

/**
 * 
 * @author Unknow
 */
@Sharable
public class SyncServ {
	private static final Logger log = LoggerFactory.getLogger(SyncServ.class);
	private Cfg cfg;

	private Map<String, Project> projects;

	private Map<UUID, State> states;

	// private Shell shell;

	public SyncServ(Cfg c) throws IOException, NoSuchAlgorithmException {
		cfg = c;
		projects = new HashMap<String, Project>();
		states = new HashMap<>();
		log.info("load");
		loadProjects();

		log.info("ready");
		// shell = new Shell(this);
		// shell.start();
	}

	/**
	 * reload a clean repertory
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws JsonException
	 */
	public void reload() throws IOException, NoSuchAlgorithmException {
		projects.clear();
		loadProjects();
	}

	private void loadProjects() throws NoSuchAlgorithmException, IOException {
		for (Entry<String, P> e : cfg.projects.entrySet())
			projects.put(e.getKey(), new Project(e.getKey(), e.getValue()));
	}

	private long lsb = 0;
	private long msb = 0;
	private Object mutex = new Object();

	public State nextState() {
		State s = new State();
		UUID uuid;
		synchronized (mutex) {
			uuid = nextUUID();
			while (states.containsKey(uuid))
				uuid = nextUUID();
			states.put(uuid, s);
		}
		s.uuid = uuid;
		return s;
	}

	private UUID nextUUID() {
		byte[] b = new byte[16];
		long m;
		long l;
		l = lsb++;
		if (lsb == -1)
			msb++;
		m = msb;

		b[0] = (byte) ((m >> 56) & 0xFF);
		b[1] = (byte) ((m >> 48) & 0xFF);
		b[2] = (byte) ((m >> 40) & 0xFF);
		b[3] = (byte) ((m >> 32) & 0xFF);
		b[4] = (byte) ((m >> 24) & 0xFF);
		b[5] = (byte) ((m >> 16) & 0xFF);
		b[6] = (byte) ((m >> 8) & 0xFF);
		b[7] = (byte) ((m) & 0xFF);
		b[8] = (byte) ((l >> 56) & 0xFF);
		b[9] = (byte) ((l >> 48) & 0xFF);
		b[10] = (byte) ((l >> 40) & 0xFF);
		b[11] = (byte) ((l >> 32) & 0xFF);
		b[12] = (byte) ((l >> 24) & 0xFF);
		b[13] = (byte) ((l >> 16) & 0xFF);
		b[14] = (byte) ((l >> 8) & 0xFF);
		b[15] = (byte) ((l) & 0xFF);

		return new UUID(b);
	}

	public static void main(String arg[]) throws NoSuchAlgorithmException, IOException, InterruptedException {
		// Cfg cfg = Cfg.getSystem();
		// if (arg.length != 0)
		// cfg = new Cfg(arg[0]);
		Cfg cfg = new ObjectMapper().readValue(new File("sync.cfg"), Cfg.class);
		System.out.println(cfg);
		SyncServ serv = new SyncServ(cfg);
		final Kryos kryos = new Kryos();
		final SyncHandler handler = new SyncHandler(serv);

		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() { // (4)
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new KryoDecoder(kryos), new KryoEncoder(kryos), handler);
				}
			}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(cfg.port).sync(); // TODO

			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public String getPass(String login) {
		return cfg.users.get(login);
	}

	public Project getProject(String project) {
		return projects.get(project);
	}

	public State getState(UUID uuid) {
		return states.get(uuid);
	}

	public Map<String, Project> projects() {
		return projects;
	}
}