package jit.x64;

class X64Jump {
	private final int byteSize;
	private final int position;

	public X64Jump(int byteSize, int position) {
		this.byteSize = byteSize;
		this.position = position;
	}

	public int getByteSize() {
		return byteSize;
	}

	public int getPosition() {
		return position;
	}
}
