package unknow.sync.proto.pojo;

import java.util.*;

public class Hash {
	public byte[] bytes;

	public Hash() {
	}

	public Hash(byte[] bytes) {
		if (bytes.length > 64)
			throw new IllegalArgumentException();
		this.bytes = bytes;
	}

	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o instanceof Hash)
			return Arrays.equals(bytes, ((Hash) o).bytes);
		if (o instanceof byte[])
			return Arrays.equals(bytes, (byte[]) o);
		return false;
	}
}
