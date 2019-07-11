package bytecode;

public class BytecodeArray extends BytecodeType {
	private static final long serialVersionUID = 1L;

	private BytecodeType elementType;

	public BytecodeArray(String identifier) {
		super(identifier);
	}

	public BytecodeType getElementType() {
		return elementType;
	}

	public void setElementType(BytecodeType elementType) {
		this.elementType = elementType;
	}
}
