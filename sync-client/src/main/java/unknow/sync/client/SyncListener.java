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
		private static final long[] DURATION = new long[] { 24 * 3600000, 3600000, 60000, 1000, 1 };
		private static final String[] UNIT = new String[] { "d", "h", "m", "s", "ms" };

		private static final Logger log = LoggerFactory.getLogger(Log.class);

		private long start;

		private final Size done = new Size();
		private final Size total = new Size();

		private final Object elapsed = new Object() {
			@Override
			public String toString() {
				StringBuilder out = new StringBuilder();
				long ms = System.currentTimeMillis() - start;
				for (int i = 0; i < DURATION.length; i++) {
					long d = DURATION[i];
					if (ms > d) {
						long v = ms / d;
						out.append(Long.toString(v));
						out.append(UNIT[i]);
						ms -= v * d;
					}
				}
				return out.toString();
			}
		};
		private final Size speed = new Size();

		@Override
		public void start() {
			log.info("Starting");
			start = System.currentTimeMillis();
		}

		@Override
		public void update(long done, long total) {
			this.done.value = done;
			this.total.value = total;

			speed.value = total * 1000 / (System.currentTimeMillis() - start);
			log.info("in progress {} / {} in {} ({}/sec)", this.done, this.total, elapsed, speed);
		}

		@Override
		public void done(long total) {
			speed.value = total * 1000 / (System.currentTimeMillis() - start);
			log.info("Done updating in {} ({}/sec)", elapsed, speed);
		}
	}

	public static class Size {
		private static final String[] UNITS = new String[] { " o", " Ko", "Mo", "Go", "To", "Eo" };

		long value;

		@Override
		public String toString() {
			double s = value;
			int i = 0;
			while (s > 1024 && i < UNITS.length) {
				s /= 1024;
				i++;
			}

			return String.format("%.2f %s", s, UNITS[i]);
		}
	}
}
