package unknow.sync.proto.pojo;

import java.util.*;

public class UUID {
	public byte[] bytes;

	public UUID() {
	}

	public UUID(byte[] bytes) {
		if (bytes.length != 16)
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
		if (o instanceof UUID)
			return Arrays.equals(bytes, ((UUID) o).bytes);
		if (o instanceof byte[])
			return Arrays.equals(bytes, (byte[]) o);
		return false;
	}
}
