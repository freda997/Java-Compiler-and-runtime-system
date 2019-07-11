package bytecode;

import java.io.Serializable;

public class BytecodeField implements Serializable {
	private static final long serialVersionUID = 1L;

	private final BytecodeClass containingClass;
	private final String identifier;
	private BytecodeType type;

	public BytecodeField(BytecodeClass containingClass, String identifier) {
		this.containingClass = containingClass;
		this.identifier = identifier;
	}

	public BytecodeClass getContainingClass() {
		return containingClass;
	}

	public String getIdentifier() {
		return identifier;
	}

	public BytecodeType getType() {
		return type;
	}

	public void setType(BytecodeType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return containingClass.getIdentifier() + "." + identifier;
	}
}
