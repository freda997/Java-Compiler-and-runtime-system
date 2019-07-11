package bytecode;

import java.io.Serializable;

public class BytecodeType implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String identifier;

	public BytecodeType(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String toString() {
		return identifier;
	}
}
