package jit.x64;

public enum X64Register {
	RAX(0), RCX(1), RDX(2), RBX(3), RSP(4), RBP(5), RSI(6), RDI(7), 
	R8(8), R9(9), R10(10), R11(11), R12(12), R13(13), R14(14), R15(15);

	private final int code;

	private X64Register(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
