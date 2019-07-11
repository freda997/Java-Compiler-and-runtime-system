package jit;

import java.util.Set;

import bytecode.OpCode;
import runtime.descriptors.MethodDescriptor;
import runtime.descriptors.TypeDescriptor;

public class JITPrecondition {
	private static final Set<OpCode> CONDITIONAL_BRANCHES = Set.of(OpCode.IF_TRUE, OpCode.IF_FALSE);
	private static final Set<OpCode> COMPARE_OPERATORS = Set.of(OpCode.CMPEQ, OpCode.CMPNE, OpCode.ICMPLT,
			OpCode.ICMPLE, OpCode.ICMPGT, OpCode.ICMPGE);
	
	
	public static boolean fulfilled(MethodDescriptor method) {
		if (method.getReturnType() != null && !supportedType(method.getReturnType())) {
			return false;
		}
		if (method.getParameterTypes().length > 4) {
			return false;
		}
		for (var parameterType : method.getParameterTypes()) {
			if (!supportedType(parameterType)) {
				return false;
			}
		}
		for (var localType : method.getLocalTypes()) {
			if (!supportedType(localType)) {
				return false;
			}
		}
		var code = method.getCode();
		for (int position = 0; position < code.length; position++) {
			var instruction = code[position];
			var operand = instruction.getOperand();
			switch (instruction.getOpCode()) {
			case LDC:
				if (!(operand instanceof Integer || operand instanceof Boolean)) {
					return false;
				}
				break;
			case LOAD:
			case STORE:
				if (!(operand instanceof Integer)) {
					return false;
				}
				var index = (int) operand;
				var parameters = method.getParameterTypes();
				var locals = method.getLocalTypes();
				if (index <= 0 || index > parameters.length + locals.length) {
					return false;
				} else if (index <= parameters.length && !supportedType(parameters[index - 1])) {
					return false;
				} else if (index > parameters.length && !supportedType(locals[index - 1 - parameters.length])) {
					return false;
				}
				break;
			case CMPEQ:
			case CMPNE:
			case ICMPLT:
			case ICMPLE:
			case ICMPGT:
			case ICMPGE:
				var next = position + 1 < code.length ? code[position + 1] : null;
				if (next == null || !CONDITIONAL_BRANCHES.contains(next.getOpCode())) {
					return false;
				}
				break;
			case IF_FALSE:
			case IF_TRUE:
				if (!(operand instanceof Integer)) {
					return false;
				}
				var previous = position > 0 ? code[position - 1] : null;
				if (previous == null || !COMPARE_OPERATORS.contains(previous.getOpCode())) {
					return false;
				}
				break;
			case GOTO:
			case IADD:
			case ISUB:
			case IMUL:
			case IDIV:
			case IREM:
			case INEG:
			case BNEG:
			case RETURN:
				break;
			default:
				return false;
			}
		}
		return true;
	}

	private static boolean supportedType(TypeDescriptor descriptor) {
		return descriptor == TypeDescriptor.BOOLEAN_TYPE || descriptor == TypeDescriptor.INT_TYPE;
	}
}
