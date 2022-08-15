/**
 * 
 */
package unknow.sync.common;

/**
 * @author unknow
 */
public class FastHash {
	private long h;
	private long r = 0;
	private int rem = 0;

	/**
	 * create new FastHash
	 */
	public FastHash() {
		h = 0xCAFEBABE;
	}

	private static long mix(long h) {
		h ^= h >>> 23;
		h *= 0x2127599bf4325c37L;
		h ^= h >>> 47;
		return h;
	}

	/**
	 * append one byte
	 * 
	 * @param b
	 */
	public void update(byte b) {
		r |= (long) b & 0xFF << (8 * rem);
		if (++rem == 8) {
			update(r);
			rem = 0;
			r = 0;
		}
	}

	/**
	 * same as {@code update(b, 0, b.length)}
	 * 
	 * @param b
	 */
	public void update(byte[] b) {
		update(b, 0, b.length);
	}

	/**
	 * update the hash with new data
	 * 
	 * @param b
	 * @param off
	 * @param len
	 */
	public void update(byte[] b, int off, int len) {
		if (len == 0)
			return;
		while (rem > 0 && len-- > 0)
			update(b[off++]);
		if (len == 0)
			return;

		rem = len & 7;
		final long end = off + len;
		final long endr = end - rem;

		while (off < endr)
			update((long) b[off++] & 0xFF | ((long) b[off++] & 0xFF) << 8 | ((long) b[off++] & 0xFF) << 16 | ((long) b[off++] & 0xFF) << 24 | ((long) b[off++] & 0xFF) << 32 | ((long) b[off++] & 0xFF) << 40 | ((long) b[off++] & 0xFF) << 48 | ((long) b[off++] & 0xFF) << 56);

		while (off < end)
			update(b[off++]);
	}

	/**
	 * reset the hash
	 */
	public void reset() {
		rem = 0;
		r = 0;
		h = 0xCAFEBABE;
	}

	/**
	 * @return the current hash value
	 */
	public long getValue() {
		long value = h;
		if (rem > 0) { // add remaining value
			value ^= mix(r);
			value *= 0x880355f21e6d1965L;
		}
		return mix(value);
	}

	private void update(long v) {
		h ^= mix(v);
		h *= 0x880355f21e6d1965L;
	}
}
