package runtime.descriptors;

import bytecode.Instruction;

public class MethodDescriptor {
	public static final MethodDescriptor HALT_METHOD = new MethodDescriptor("halt");
	public static final MethodDescriptor WRITE_INT_METHOD = new MethodDescriptor("writeInt");
	public static final MethodDescriptor WRITE_STRING_METHOD = new MethodDescriptor("writeString");
	public static final MethodDescriptor READ_INT_METHOD = new MethodDescriptor("readInt");
	public static final MethodDescriptor READ_STRING_METHOD = new MethodDescriptor("readString");

	private final String identifier;

	private int position;
	private TypeDescriptor returnType;
	private TypeDescriptor[] parameterTypes;
	private TypeDescriptor[] localTypes;
	private Instruction[] code;

	public MethodDescriptor(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int index) {
		this.position = index;
	}

	public TypeDescriptor getReturnType() {
		return returnType;
	}

	public void setReturnType(TypeDescriptor returnType) {
		this.returnType = returnType;
	}

	public TypeDescriptor[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(TypeDescriptor[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public TypeDescriptor[] getLocalTypes() {
		return localTypes;
	}

	public void setLocalTypes(TypeDescriptor[] localTypes) {
		this.localTypes = localTypes;
	}

	public Instruction[] getCode() {
		return code;
	}

	public void setCode(Instruction[] code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return identifier;
	}
}
