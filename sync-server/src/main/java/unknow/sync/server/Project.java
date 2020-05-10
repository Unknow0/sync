package unknow.sync.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.sync.FileDescLoader;
import unknow.sync.proto.pojo.Action;
import unknow.sync.proto.pojo.FileDesc;
import unknow.sync.proto.pojo.FileHash;
import unknow.sync.proto.pojo.ProjectInfo;

public class Project {
	private static final Logger log = LoggerFactory.getLogger(Project.class);
	private List<FileDesc> files;
	private String name;
	private Cfg.P cfg;
	private Path root;

	private ProjectInfo projectInfo;

	public Project(String prj, Cfg.P cfg) throws IOException {
		name = prj;
		this.cfg = cfg;

		root = Paths.get(cfg.path);

		if (!Files.exists(root)) {
			log.warn("root directory of '{}' not found creating '{}'", name, cfg.path);
			Files.createDirectories(root);
		}
		if (Files.isDirectory(root)) {
			files = new ArrayList<>();
			projectInfo = new ProjectInfo(cfg.bloc_size, null);
			FileDescLoader.load(files, root, projectInfo.blocSize, null);

			projectInfo.hashs = new FileHash[files.size()];
			int i = 0;
			for (FileDesc fd : files)
				projectInfo.hashs[i++] = new FileHash(fd.name, fd.fileHash);
		} else
			throw new FileNotFoundException("project '" + name + "' path isn't a directory");
	}

	public boolean asRight(String login, Action action) {
		Set<String> set = cfg.rights.get(action);
		return set != null && (set.contains("all") || set.contains(login));
	}

	public int blocSize() {
		return projectInfo.blocSize;
	}

	public String path() {
		return cfg.path;
	}

	public FileDesc fileDesc(int i) {
		synchronized (this) {
			return files.get(i);
		}
	}

	public List<FileDesc> fileDesc() {
		synchronized (this) {// TODO change to readWriteLock
			return files;
		}
	}

	public ProjectInfo projectInfo() {
		synchronized (this) {
			return projectInfo;
		}
	}

	public FileDesc reloadFile(String file) throws FileNotFoundException, IOException {
		FileDesc fd = FileDescLoader.loadFile(root, Paths.get(file), projectInfo.blocSize);
		synchronized (this) {
			for (int i = 0; i < projectInfo.hashs.length; i++) {
				if (fd.name.equals(projectInfo.hashs[i].name)) {
					files.set(i, fd);
					projectInfo.hashs[i] = new FileHash(fd.name, fd.fileHash);
					return fd;
				}
			}
			projectInfo.hashs = Arrays.copyOf(projectInfo.hashs, projectInfo.hashs.length + 1);
			projectInfo.hashs[projectInfo.hashs.length - 1] = new FileHash(fd.name, fd.fileHash);
			files.add(fd);
		}
		return fd;
	}

	public boolean delete(String file) {
		synchronized (this) {
			Iterator<FileDesc> it1 = files.iterator();
			int i = 0;
			while (it1.hasNext()) {
				it1.next();
				FileHash next = projectInfo.hashs[i++];
				if (next.name.equals(file)) {
					try {
						Files.delete(root.resolve(file));

						it1.remove();
						projectInfo.hashs[i - 1] = projectInfo.hashs[projectInfo.hashs.length - 1];
						projectInfo.hashs = Arrays.copyOf(projectInfo.hashs, projectInfo.hashs.length - 1);
						return true;
					} catch (IOException e) {
						log.warn("failed to delete '" + file + "'", e);
						return false;
					}
				}
			}
		}

		return false;
	}
}