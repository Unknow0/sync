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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.sync.FileUtils;
import unknow.sync.proto.pojo.FileDesc;
import unknow.sync.proto.pojo.ProjectInfo;
import unknow.sync.proto.pojo.ProjectInfo.FileInfo;

public class Project {
	private static final Logger log = LoggerFactory.getLogger(Project.class);
	private Map<String, FileDesc> files;
	private Path root;

	private ProjectInfo projectInfo;

	private Set<String> tokens;

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

		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FileDesc t = FileUtils.loadFile(root, file, cfg.blocSize);
				files.put(t.name, t);
				return super.visitFile(file, attrs);
			}
		});
		projectInfo.files = new FileInfo[files.size()];
		int i = 0;
		int c = 0;
		for (FileDesc fd : files.values()) {
			projectInfo.files[i++] = new FileInfo(fd.name, fd.size, fd.hash); // XXX
			c += fd.blocs.length;
		}

		log.debug("done {} found ({} blocs)", files.size(), c);

		tokens = cfg.tokens;
	}

	public int blocSize() {
		return projectInfo.blocSize;
	}

	public Path file(String file) throws IOException {
		Path f = root.resolve(file).toAbsolutePath();
		if (!f.startsWith(root))
			throw new IOException("try to get file outside root");
		return f;
	}

	public FileDesc fileDesc(String name) {
		return files.get(name);
	}

	public ProjectInfo projectInfo() {
		synchronized (this) {
			return projectInfo;
		}
	}

	public boolean asRight(String token) {
		return tokens.contains(token);
	}

}