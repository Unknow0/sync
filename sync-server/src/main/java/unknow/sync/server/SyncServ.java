package unknow.sync.server;

import java.io.IOException;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import unknow.log.Log;
import unknow.log.LogFactory;

/**
 * 
 * @author Unknow
 */
public class SyncServ {
	private static final Log log = LogFactory.getLogger(SyncServ.class);

	private final ChannelFuture f;
	private final EventLoopGroup bossGroup = new NioEventLoopGroup();
	private final EventLoopGroup workerGroup = new NioEventLoopGroup();

	/**
	 * create new SyncServ
	 * 
	 * @param cfg
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public SyncServ(Cfg cfg) throws InterruptedException, IOException {
		log.info("starting up ... ");
		Project serv = new Project(cfg);

		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() { // (4)
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				log.debug("new client {}", ch);

				ch.pipeline().addLast(new SyncHandler(serv));
			}
		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
		// Bind and start to accept incoming connections.
		f = b.bind(cfg.port).sync();
	}

	/**
	 * wait for the server to be closed
	 * 
	 * @throws InterruptedException
	 */
	public void awaitClose() throws InterruptedException {
		f.channel().closeFuture().sync();
	}

	/**
	 * close the server (unlock all thread in {@link SyncServ#awaitClose()})
	 */
	public void close() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	/**
	 * @param arg
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String arg[]) throws IOException, InterruptedException {
		Cfg cfg = new Cfg();
		OptionHandlerRegistry.getRegistry().registerHandler(Set.class, Cfg.SetOption.class);
		CmdLineParser cmdLineParser = new CmdLineParser(cfg);
		try {
			cmdLineParser.parseArgument(arg);
		} catch (CmdLineException e) {
			cmdLineParser.printUsage(System.out);
			System.exit(1);
		}
		SyncServ syncServ = null;
		try {
			syncServ = new SyncServ(cfg);
			log.info("done, waiting client");
			syncServ.awaitClose();
		} finally {
			if (syncServ != null)
				syncServ.close();
		}
	}
}