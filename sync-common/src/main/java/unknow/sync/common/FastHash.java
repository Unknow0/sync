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
		if (rem > 0) {
			rem = 8 - rem;
			switch (rem) {
				case 7:
					r |= ((long) b[off++] & 0xFF) << 8;
				case 6:
					r |= ((long) b[off++] & 0xFF) << 16;
				case 5:
					r |= ((long) b[off++] & 0xFF) << 24;
				case 4:
					r |= ((long) b[off++] & 0xFF) << 32;
				case 3:
					r |= ((long) b[off++] & 0xFF) << 40;
				case 2:
					r |= ((long) b[off++] & 0xFF) << 48;
				case 1:
					r |= ((long) b[off++] & 0xFF) << 56;
			}
			update(r);
			len -= rem;
		}
		rem = len & 7;
		final long end = off + len;
		final long endr = end - rem;

		while (off < endr)
			update((long) b[off++] & 0xFF | ((long) b[off++] & 0xFF) << 8 | ((long) b[off++] & 0xFF) << 16 | ((long) b[off++] & 0xFF) << 24 | ((long) b[off++] & 0xFF) << 32 | ((long) b[off++] & 0xFF) << 40 | ((long) b[off++] & 0xFF) << 48 | ((long) b[off++] & 0xFF) << 56);

		r = 0;
		if (off < end)
			r |= (long) b[off++] & 0xFF;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 8;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 16;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 24;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 32;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 40;
		if (off < end)
			r |= ((long) b[off++] & 0xFF) << 48;
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
