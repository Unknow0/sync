/**
 * 
 */
package unknow.sync;

import java.util.Arrays;
import java.util.Random;

/**
 * @author unknow
 */
public class FastHash {
	private long h;
	private long r = 0;
	private int rem = 0;

	public FastHash() {
		h = 0xCAFEBABE;
	}

	private static long mix(long h) {
		h ^= h >>> 23;
		h *= 0x2127599bf4325c37L;
		h ^= h >>> 47;
		return h;
	}

	public void update(byte[] b) {
		update(b, 0, b.length);
	}

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
		}
		len -= rem;
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

	public void reset() {
		rem = 0;
		h = 0xCAFEBABE;
	}

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

	public static void main(String[] arg) throws Exception {

		final int CHECKS = 1000000;
		final int MAX_LENGTH = 1 << 12;

		Random random = new Random();
		long[] checksum = new long[CHECKS];
		int len, i, j;

		FastHash fastHash = new FastHash();
		for (len = 1; len < MAX_LENGTH; len <<= 1) {
			int id = 0;
			long collisions = 0;
			long ref = CHECKS / 2 * (CHECKS + 1);
			byte[] b = new byte[len];

			for (i = 0; i < CHECKS; i++) {
				fastHash.reset();
				for (j = 0; j < len; j++)
					b[j] = (byte) (random.nextInt(256) & 0xFF);
				fastHash.update(b);
				checksum[i] = fastHash.getValue();
			}
			Arrays.sort(checksum);
			for (i = 1; i < CHECKS; i++) {
				if (checksum[i] == checksum[i - 1]) {
					id++;
					collisions += id;
				} else
					id = 0;
			}
			collisions += id;
			System.out.format("len=%6d, collisions=%9d(%f%%), effective bits=%f\n", len, collisions, 100.0 * collisions / ref, Math.log(ref / (double) collisions) / Math.log(2));
		}
	}
}
