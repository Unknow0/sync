package unknow.sync;

import org.slf4j.*;

public interface SyncListener {
	public void startUpdate(String project, int modified, int news, int delete);

	public void startFile(String name);

	public void startCheckFile(String name);

	public void doneCheckFile(String name);

	public void startReconstruct(String name);

	public void updateReconstruct(String name, float rate);

	public void doneReconstruct(String name, long fileSize, boolean ok);

	public void doneFile(String name, long fileSize);

	public void doneUpdate(String project);

	public static class Log implements SyncListener {
		private static final Logger log = LoggerFactory.getLogger(Log.class);

		public long start;
		public long file;
		public long local;

		public long totalSize;

		@Override
		public void startUpdate(String project, int updated, int news, int delete) {
			log.info("Start updating '{}', ({} to update, {} to add, {} to remove", project, updated, news, delete);
			start = System.currentTimeMillis();
		}

		@Override
		public void startFile(String name) {
			log.info("Starting '{}'", name);
			file = System.currentTimeMillis();
		}

		@Override
		public void startCheckFile(String name) {
			log.info("Starting diff for '{}'", name);
			local = System.currentTimeMillis();
		}

		@Override
		public void doneCheckFile(String name) {
			log.info("Finished diff for '{}' in {} sec", name, String.format("%.3f", (System.currentTimeMillis() - local) / 1000.));
		}

		@Override
		public void startReconstruct(String name) {
			log.info("Start reconstructing '{}'", name);
			local = System.currentTimeMillis();
		}

		@Override
		public void updateReconstruct(String name, float rate) {
			// TODO Auto-generated method stub

		}

		@Override
		public void doneReconstruct(String name, long fileSize, boolean ok) {
			double sec = (System.currentTimeMillis() - local) / 1000.;
			log.info("Finished reconstructing '{}' in {} sec ({} Ko/sec)", name, String.format("%.3f", sec), String.format("%.3f", fileSize / 1024. / sec));
			if (!ok)
				log.warn("File '{}' hash missmatched retry.", name);
		}

		@Override
		public void doneFile(String name, long fileSize) {
			double sec = (System.currentTimeMillis() - file) / 1000.;
			totalSize += fileSize;
			log.info("Finished update for '{}' in {} sec ({} Ko/sec)", name, String.format("%.3f", sec), String.format("%.3f", fileSize / 1024. / sec));
		}

		@Override
		public void doneUpdate(String project) {
			double sec = (System.currentTimeMillis() - start) / 1000.;
			log.info("Done updating '{}' in {} ({} Ko/sec)", project, String.format("%.3f", sec), String.format("%.3f", totalSize / 1024. / sec));
		}
	}
}
