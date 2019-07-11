package runtime.heap;

public final class Pointer {
	private final long address;

	public Pointer(long address) {
		this.address = address;
	}

	public long getAddress() {
		return address;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		var other = (Pointer)obj;
		return other.address == address;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(address);
	}

	@Override
	public String toString() {
		return "Pointer(" + address + ")";
	}
}
