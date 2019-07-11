package bytecode;

import java.io.Serializable;

public class Instruction implements Serializable {
	private static final long serialVersionUID = 1L;

	private final OpCode opCode;
	private Object operand;

	public Instruction(OpCode opCode, Object operand) {
		this.opCode = opCode;
		this.operand = operand;
	}

	public OpCode getOpCode() {
		return opCode;
	}

	public Object getOperand() {
		return operand;
	}

	public void setOperand(Object operand) {
		this.operand = operand;
	}
	
	@Override
	public String toString() {
		var text = opCode.toString();
		if (operand != null) {
			String name;
			if (operand instanceof BytecodeType) {
				name = ((BytecodeType)operand).getIdentifier();
			} else if (operand instanceof BytecodeMethod) {
				name = ((BytecodeMethod)operand).getIdentifier();
			} else {
				name = operand.toString();
			}
			text += " " + name;
		}
		return text;
	}
}
