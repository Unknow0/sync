package unknow.sync.client;

import unknow.log.LogFactory;

/**
 * Update listener
 * 
 * @author unknow
 */
public interface SyncListener {
	/** start the update */
	public void start();

	/**
	 * the value changed
	 * 
	 * @param done  byte done
	 * @param total total byte to process
	 */
	public void update(long done, long total);

	/**
	 * update done
	 * 
	 * @param total total byte processed
	 */
	public void done(long total);

	/**
	 * log all update
	 */
	public static class Log implements SyncListener {
		private static final unknow.log.Log log = LogFactory.getLogger(Log.class);

		private long start;

		private long done;
		private long total;

		@Override
		public void start() {
			log.info("Starting");
			start = System.currentTimeMillis();
		}

		@Override
		public void update(long done, long total) {
			this.done = done;
			this.total = total;
			long elapsed = System.currentTimeMillis() - start;
			log.info("in progress {,size} / {,size} in {,duration} ({,size}/sec)", this.done, this.total, elapsed, done * 1000. / elapsed);
		}

		@Override
		public void done(long total) {
			double elapsed = System.currentTimeMillis() - start;
			log.info("Done updating in {,duration} ({,size}/sec)", elapsed, total * 1000. / elapsed);
		}
	}
}
