package bytecode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BytecodeMethod implements Serializable {
	private static final long serialVersionUID = 1L;

	private final BytecodeClass containingClass;
	private final String identifier;
	private BytecodeType returnType;
	private final List<BytecodeType> parameterTypes = new ArrayList<>();
	private final List<BytecodeType> localTypes = new ArrayList<>();
	private final List<Instruction> code = new ArrayList<>();

	public BytecodeMethod(BytecodeClass containingClass, String identifier) {
		this.containingClass = containingClass;
		this.identifier = identifier;
	}

	public BytecodeClass getContainingClass() {
		return containingClass;
	}

	public String getIdentifier() {
		return identifier;
	}

	public BytecodeType getReturnType() {
		return returnType;
	}

	public void setReturnType(BytecodeType returnType) {
		this.returnType = returnType;
	}

	public List<BytecodeType> getParameterTypes() {
		return parameterTypes;
	}

	public List<BytecodeType> getLocalTypes() {
		return localTypes;
	}

	public List<Instruction> getCode() {
		return code;
	}

	@Override
	public String toString() {
		var text = "METHOD " + returnType + " " + identifier + "(";
		for (int index = 0; index < parameterTypes.size(); index++) {
			text += parameterTypes.get(index);
			if (index < parameterTypes.size() - 1) {
				text += ", ";
			}
		}
		text += ") " + localTypes + "\r\nCODE: {\r\n";
		for (var instruction : code) {
			text += instruction + "\r\n";
		}
		text += "}";
		return text;
	}
}
