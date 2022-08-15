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
import unknow.sync.proto.FileInfo;
import unknow.sync.proto.ProjectInfo;

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
	 * @param path     root path of the project
	 * @param blocSize blockSize
	 * @param tokens   allowed token
	 * @throws IOException
	 */
	public Project(String path, int blocSize, Set<String> tokens) throws IOException {
		root = Paths.get(path).toAbsolutePath();

		if (!Files.exists(root)) {
			log.warn("root directory not found creating '{}'", root);
			Files.createDirectories(root);
		}
		if (!Files.isDirectory(root))
			throw new FileNotFoundException("path '" + root + "' isn't a directory");

		files = new HashMap<>();
		ProjectInfo.Builder p = ProjectInfo.newBuilder().setBlocSize(blocSize);
		log.debug("loading filedesc");

		AtomicLong size = new AtomicLong();
		long start = System.currentTimeMillis();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FileDesc t = FileUtils.loadFile(root, file, blocSize);
				files.put(t.name, t);
				size.set(size.get() + t.size);
				log.debug("blocs found: {} {}", t.name, t.blocs.size());
				return FileVisitResult.CONTINUE;
			}
		});
		log.debug("done {} in {}", size, System.currentTimeMillis() - start);
		for (FileDesc fd : files.values())
			p.addFile(FileInfo.newBuilder().setName(fd.name).setSize(fd.size).setHash(fd.hash));

		this.projectInfo = p.build();
		this.tokens = tokens;
	}

	/** @return the bloc size */
	public int blocSize() {
		return projectInfo.getBlocSize();
	}

	/**
	 * @param file
	 * @return the path of a file
	 * @throws IOException if the file is not in this project
	 */
	public Path file(String file) throws IOException {
		Path f = root.resolve(file).toAbsolutePath();
		if (!f.startsWith(root) || !Files.isRegularFile(f) || !Files.isReadable(f))
			return null;
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
		return projectInfo;
	}

	/**
	 * @param token
	 * @return true if the token is found
	 */
	public boolean asRight(String token) {
		return tokens.contains(token);
	}

}