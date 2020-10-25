package unknow.sync.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.sync.common.FileUtils;
import unknow.sync.common.pojo.FileDesc;
import unknow.sync.common.pojo.FileInfo;
import unknow.sync.common.pojo.ProjectInfo;

/** data for a project */
public class Project {
	private static final Logger log = LoggerFactory.getLogger(Project.class);
	private Map<String, FileDesc> files;
	private Path root;

	private ProjectInfo projectInfo;

	private Set<String> tokens;

	/**
	 * create new Project
	 * 
	 * @param cfg
	 * @throws IOException
	 */
	public Project(Cfg cfg) throws IOException {
		root = Paths.get(cfg.path).toAbsolutePath();

		if (!Files.exists(root)) {
			log.warn("root directory not found creating '{}'", root);
			Files.createDirectories(root);
		}
		if (!Files.isDirectory(root))
			throw new FileNotFoundException("path '" + root + "' isn't a directory");

		files = new HashMap<>();
		projectInfo = new ProjectInfo(cfg.blocSize, null);
		log.debug("loading filedesc");

		AtomicLong size = new AtomicLong();
		long start = System.currentTimeMillis();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FileDesc t = FileUtils.loadFile(root, file, cfg.blocSize);
				files.put(t.name, t);
				size.set(size.get() + t.size);
				return FileVisitResult.CONTINUE;
			}
		});
		log.debug("done {} in {}", size, System.currentTimeMillis() - start);
		projectInfo.files = new FileInfo[files.size()];
		int i = 0;
		for (FileDesc fd : files.values())
			projectInfo.files[i++] = new FileInfo(fd.name, fd.size, fd.hash); // XXX

		tokens = cfg.tokens;
	}

	/** @return the bloc size */
	public int blocSize() {
		return projectInfo.blocSize;
	}

	/**
	 * @param file
	 * @return the path of a file
	 * @throws IOException if the file is not in this project
	 */
	public Path file(String file) throws IOException {
		Path f = root.resolve(file).toAbsolutePath();
		if (!f.startsWith(root))
			throw new IOException("try to get file outside root");
		if (!Files.exists(f))
			throw new IOException("file '" + f + "' doesn't exists");
		return f;
	}

	/**
	 * @param name
	 * @return the FileDesc
	 */
	public FileDesc fileDesc(String name) {
		return files.get(name);
	}

	/** @return the project info */
	public ProjectInfo projectInfo() {
		synchronized (this) {
			return projectInfo;
		}
	}

	/**
	 * @param token
	 * @return true if the token is found
	 */
	public boolean asRight(String token) {
		return tokens.contains(token);
	}

}