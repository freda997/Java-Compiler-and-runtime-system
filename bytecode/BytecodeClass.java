package bytecode;

import java.util.ArrayList;
import java.util.List;

public class BytecodeClass extends BytecodeType {
	private static final long serialVersionUID = 1L;

	private BytecodeClass baseType;
	private final List<BytecodeField> fields = new ArrayList<>();
	private final List<BytecodeMethod> methods = new ArrayList<>();

	public BytecodeClass(String identifier) {
		super(identifier);
	}

	public BytecodeClass getBaseType() {
		return baseType;
	}

	public void setBaseType(BytecodeClass baseType) {
		this.baseType = baseType;
	}

	public List<BytecodeField> getFields() {
		return fields;
	}

	public List<BytecodeMethod> getMethods() {
		return methods;
	}
}
