package unknow.sync;

import org.apache.logging.log4j.*;

public interface SyncListener
	{
	public void startUpdate(String project, int modified, int news, int delete);

	public void startFile(String name);

	public void startCheckFile(String name);

	public void doneCheckFile(String name);

	public void startReconstruct(String name);

	public void updateReconstruct(String name, float rate);

	public void doneReconstruct(String name, long fileSize, boolean ok);

	public void doneFile(String name, long fileSize);

	public void doneUpdate(String project);

	public static class Log implements SyncListener
		{
		private static final Logger log=LogManager.getFormatterLogger(Log.class);

		public long start;
		public long file;
		public long local;

		public long totalSize;

		public void startUpdate(String project, int updated, int news, int delete)
			{
			log.info("Start updating '%s', (%d to update, %d to add, %d to remove", project, updated, news, delete);
			start=System.currentTimeMillis();
			}

		public void startFile(String name)
			{
			log.info("Starting '%s'", name);
			file=System.currentTimeMillis();
			}

		public void startCheckFile(String name)
			{
			log.info("Starting diff for '%s'", name);
			local=System.currentTimeMillis();
			}

		public void doneCheckFile(String name)
			{
			log.info("Finished diff for '%s' in %.3f sec", name, (System.currentTimeMillis()-local)/100.);
			}

		public void startReconstruct(String name)
			{
			log.info("Start reconstructing '%s'", name);
			local=System.currentTimeMillis();
			}

		public void updateReconstruct(String name, float rate)
			{
			// TODO Auto-generated method stub

			}

		public void doneReconstruct(String name, long fileSize, boolean ok)
			{
			double sec=(System.currentTimeMillis()-local)/100.;
			log.info("Finished reconstructing '%s' in %.3f sec (%.3f Ko/sec)", name, sec, fileSize/1024./sec);
			if(!ok)
				log.warn("File '%s' hash missmatched retry.", name);
			}

		public void doneFile(String name, long fileSize)
			{
			double sec=(System.currentTimeMillis()-file)/100.;
			totalSize+=fileSize;
			log.info("Finished update for '%s' in %.3f sec (%.3f Ko/sec)", name, sec, fileSize/1024./sec);
			}

		public void doneUpdate(String project)
			{
			double sec=(System.currentTimeMillis()-start)/100.;
			log.info("Done updating '%s' in %.3f (%.3f Ko/sec)", project, sec, totalSize/1024./sec);
			}
		}
	}
