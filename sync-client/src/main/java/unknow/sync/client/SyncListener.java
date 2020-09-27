package unknow.sync.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		private static final Logger log = LoggerFactory.getLogger(Log.class);

		private long start;

		private final Size done = new Size(0);
		private final Size total = new Size(0);

		private final Object elapsed = new Object() {
			@Override
			public String toString() {
				double sec = (System.currentTimeMillis() - start) / 1000.;
				// TODO time format
				return String.format("%.3f sec", sec);
			}
		};
		private final Object speed = new Object() {
			@Override
			public String toString() {
				double sec = (System.currentTimeMillis() - start) / 1000.;
				double s = done.s / sec;
				if (s < 1024)
					return s + " o/sec";
				s = s / 1024.;
				if (s < 1024)
					return String.format("%.2f Ko/s", s);
				s = s / 1024.;
				if (s < 1024)
					return String.format("%.2f Mo/s", s);
				s = s / 1024.;
				if (s < 1024)
					return String.format("%.2f Go/s", s);
				return String.format("%.2f To/s", s / 1024.);
			}
		};

		@Override
		public void start() {
			log.info("Starting");
			start = System.currentTimeMillis();
		}

		@Override
		public void update(long done, long total) {
			this.done.s = done;
			this.total.s = total;
			log.info("in progress {} / {} in {} ({})", this.done, this.total, elapsed, speed);
		}

		@Override
		public void done(long total) {
			this.total.s = total;
			log.info("Done updating in {} ({})", elapsed, speed);
		}
	}

	/** byte formating */
	public static class Size {
		/** value to format */
		public long s;

		/**
		 * new Size
		 * 
		 * @param s the value
		 */
		public Size(long s) {
			this.s = s;
		}

		@Override
		public String toString() {
			if (s < 1024)
				return s + " o";
			double d = s / 1024.;
			if (d < 1024)
				return String.format("%.2f Ko", d);
			d = d / 1024.;
			if (d < 1024)
				return String.format("%.2f Mo", d);
			d = d / 1024.;
			if (d < 1024)
				return String.format("%.2f Go", d);
			return String.format("%.2f To", d / 1024.);
		}
	}
}
