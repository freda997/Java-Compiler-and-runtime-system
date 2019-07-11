package runtime;

import java.util.Objects;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;

import bytecode.Instruction;
import error.InvalidBytecodeException;
import error.VMException;
import jit.JITCompiler;
import jit.JITNative;
import jit.JITPrecondition;
import runtime.descriptors.ArrayDescriptor;
import runtime.descriptors.ClassDescriptor;
import runtime.descriptors.FieldDescriptor;
import runtime.descriptors.MethodDescriptor;
import runtime.descriptors.TypeDescriptor;
import runtime.heap.Heap;
import runtime.heap.Pointer;

public class Interpreter {
	private final Loader loader;
	private final CallStack callStack = new CallStack();
	private final Heap heap;
	private final Scanner input = new Scanner(System.in);
	private final boolean useJIT;

	public Interpreter(Loader loader, boolean useJIT) {
		Objects.requireNonNull(loader);
		this.loader = loader;
		heap = new Heap(callStack);
		this.useJIT = useJIT;
	}

	public void run() {
		setProgramEntry();
		while (!callStack.isEmpty()) {
			step();
		}
	}

	private void setProgramEntry() {
		var mainMethod = loader.getMainMethod();
		var mainClass = loader.getMainClass();
		if (mainMethod.getReturnType() != null || mainMethod.getParameterTypes().length != 0) {
			throw new InvalidBytecodeException("Invalid main method");
		}
		var mainObject = newObject(mainClass);
		invokeVirtual(mainMethod, mainObject, new Object[0]);
	}

	private void step() {
		var frame = activeFrame();
		var code = frame.getMethod().getCode();
		var pointer = frame.getInstructionPointer();
		if (pointer == code.length) {
			throw new VMException("Return statement missing");
		} else if (pointer < 0 || pointer >= code.length) {
			throw new InvalidBytecodeException("Invalid instruction pointer");
		}
		var instruction = code[pointer];
		frame.setInstructionPointer(pointer + 1);
		execute(instruction);
	}

	private void execute(Instruction instruction) {
		var operand = instruction.getOperand();
		switch (instruction.getOpCode()) {
		case LDC:
			push(operand);
			break;
		case ACONST_NULL:
			push(null);
			break;
		case IADD:
			arithmetic((x, y) -> x + y);
			break;
		case ISUB:
			arithmetic((x, y) -> x - y);
			break;
		case IMUL:
			arithmetic((x, y) -> x * y);
			break;
		case IDIV:
			arithmetic((x, y) -> x / y);
			break;
		case IREM:
			arithmetic((x, y) -> x % y);
			break;
		case INEG:
			push(-checkInt(pop()));
			break;
		case BNEG:
			push(!checkBoolean(pop()));
			break;
		case CMPEQ:
			compareAny((x, y) -> Objects.equals(x, y));
			break;
		case CMPNE:
			compareAny((x, y) -> !Objects.equals(x, y));
			break;
		case ICMPLT:
			compareInt((x, y) -> x < y);
			break;
		case ICMPLE:
			compareInt((x, y) -> x <= y);
			break;
		case ICMPGT:
			compareInt((x, y) -> x > y);
			break;
		case ICMPGE:
			compareInt((x, y) -> x >= y);
			break;
		case IF_TRUE:
			if (checkBoolean(pop())) {
				branch(operand);
			}
			break;
		case IF_FALSE:
			if (!checkBoolean(pop())) {
				branch(operand);
			}
			break;
		case GOTO:
			branch(operand);
			break;
		case INSTANCEOF:
			instanceofTest(operand);
			break;
		case CHECKCAST:
			checkCast(operand);
			break;
		case LOAD:
			loadLocal(operand);
			break;
		case STORE:
			storeLocal(operand);
			break;
		case GETFIELD:
			getField(operand);
			break;
		case PUTFIELD:
			putField(operand);
			break;
		case ALOAD:
			arrayLoad();
			break;
		case ASTORE:
			arrayStore();
			break;
		case NEW:
			newObject(operand);
			break;
		case ARRAYLENGTH:
			arrayLength();
			break;
		case NEWARRAY:
			newArray(operand);
			break;
		case INVOKESTATIC:
			invokeStatic(operand);
			break;
		case INVOKEVIRTUAL:
			invokeVirtual(operand);
			break;
		case RETURN:
			returnCall();
			break;
		default:
			throw new InvalidBytecodeException("Unsupported instruction opcode");
		}
	}

	private void returnCall() {
		var frame = activeFrame();
		var returnType = frame.getMethod().getReturnType();
		Object result = null;
		if (returnType != null) {
			if (frame.getEvaluationStack().isEmpty()) {
				throw new VMException("Return statement missing");
			}
			result = pop();
			checkType(result, returnType);
		}
		if (!frame.getEvaluationStack().isEmpty()) {
			throw new InvalidBytecodeException("Stack not empty on return");
		}
		callStack.pop();
		if (returnType != null) {
			push(result);
		}
	}

	private void arithmetic(IntBinaryOperator op) {
		var right = checkInt(pop());
		var left = checkInt(pop());
		var result = op.applyAsInt(left, right);
		push(result);
	}

	private void compareAny(BiFunction<Object, Object, Boolean> op) {
		var right = pop();
		var left = pop();
		var result = op.apply(left, right);
		push(result);
	}

	private void compareInt(BiFunction<Integer, Integer, Boolean> op) {
		var right = checkInt(pop());
		var left = checkInt(pop());
		var result = op.apply(left, right);
		push(result);
	}

	private void branch(Object operand) {
		int offset = checkInt(operand);
		var frame = activeFrame();
		frame.setInstructionPointer(frame.getInstructionPointer() + offset);
	}

	private void loadLocal(Object operand) {
		int index = checkInt(operand);
		var frame = activeFrame();
		int nofParameters = frame.getParameters().length;
		int nofLocals = frame.getLocals().length;
		if (index < 0 || index > nofParameters + nofLocals) {
			throw new InvalidBytecodeException("Invalid load index");
		}
		if (index == 0) {
			push(frame.getThisReference());
		} else if (index <= nofParameters) {
			push(frame.getParameters()[index - 1]);
		} else {
			push(frame.getLocals()[index - 1 - nofParameters]);
		}
	}

	private void storeLocal(Object operand) {
		int index = checkInt(operand);
		var frame = activeFrame();
		int nofParameters = frame.getParameters().length;
		int nofLocals = frame.getLocals().length;
		if (index <= 0 || index > nofParameters + nofLocals) {
			throw new InvalidBytecodeException("Invalid store index");
		}
		var method = frame.getMethod();
		var value = pop();
		if (index <= nofParameters) {
			checkType(value, method.getParameterTypes()[index - 1]);
			frame.getParameters()[index - 1] = value;
		} else {
			checkType(value, method.getLocalTypes()[index - 1 - nofParameters]);
			frame.getLocals()[index - 1 - nofParameters] = value;
		}
	}

	private void newArray(Object operand) {
		if (!(operand instanceof ArrayDescriptor)) {
			throw new InvalidBytecodeException("newarray has no array type operand");
		}
		var arrayType = (ArrayDescriptor) operand;
		var length = checkInt(pop());
		if (length < 0) {
			throw new VMException("Negative array length");
		}
		var array = heap.allocateArray(arrayType, length);
		var defaultValue = defaultValue(arrayType.getElementType());
		for (int index = 0; index < length; index++) {
			heap.writeElement(array, index, defaultValue);
		}
		push(array);
	}

	private void arrayLength() {
		var array = checkPointer(pop());
		if (array == null) {
			throw new VMException("Null dereferenced");
		}
		push(heap.getArrayLength(array));
	}

	private void arrayLoad() {
		var index = checkInt(pop());
		var array = checkPointer(pop());
		checkArrayIndex(array, index);
		var value = heap.readElement(array, index);
		push(value);
	}

	private void arrayStore() {
		var value = pop();
		var index = checkInt(pop());
		var array = checkPointer(pop());
		checkArrayIndex(array, index);
		var descriptor = heap.getDescriptor(array);
		if (!(descriptor instanceof ArrayDescriptor)) {
			throw new InvalidBytecodeException("astore does not refer to an array");
		}
		var elementType = ((ArrayDescriptor) descriptor).getElementType();
		checkType(value, elementType);
		heap.writeElement(array, index, value);
	}

	private void checkArrayIndex(Pointer array, int index) {
		if (array == null) {
			throw new VMException("Null dereferenced");
		}
		if (index < 0 || index >= heap.getArrayLength(array)) {
			throw new VMException("Array index out of range");
		}
	}

	private void instanceofTest(Object operand) {
		var instance = checkPointer(pop());
		if (instance == null) {
			push(false);
		} else {
			if (!(operand instanceof ClassDescriptor)) {
				throw new InvalidBytecodeException("instanceof has no class operand");
			}
			var targetType = (ClassDescriptor) operand;
			push(typeTest(instance, targetType));
		}
	}

	private void checkCast(Object operand) {
		var instance = checkPointer(pop());
		push(instance);
		if (!(operand instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("checkcast has no class operand");
		}
		var targetType = (ClassDescriptor) operand;
		if (!typeTest(instance, targetType)) {
			throw new VMException("Invalid cast");
		}
	}

	private void getField(Object operand) {
		if (!(operand instanceof FieldDescriptor)) {
			throw new InvalidBytecodeException("getfield has no field operand");
		}
		var field = (FieldDescriptor) operand;
		var instance = checkPointer(pop());
		if (instance == null) {
			throw new VMException("Null dereferenced");
		}
		var classType = getClassDescriptor(instance);
		var fieldTypes = classType.getAllFields();
		var index = field.getIndex();
		if (index < 0 || index >= fieldTypes.length || fieldTypes[index] != field) {
			throw new InvalidBytecodeException("Invalid field operand");
		}
		var value = heap.readField(instance, index);
		push(value);
	}

	private void putField(Object operand) {
		if (!(operand instanceof FieldDescriptor)) {
			throw new InvalidBytecodeException("putfield has no field operand");
		}
		var field = (FieldDescriptor) operand;
		var value = pop();
		var instance = checkPointer(pop());
		if (instance == null) {
			throw new VMException("Null dereferenced");
		}
		var classType = getClassDescriptor(instance);
		var allFields = classType.getAllFields();
		var index = field.getIndex();
		if (index < 0 || index >= allFields.length || allFields[index] != field) {
			throw new InvalidBytecodeException("Invalid field operand");
		}
		checkType(value, field.getType());
		heap.writeField(instance, index, value);
	}

	private void newObject(Object operand) {
		if (!(operand instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("new has no class operand");
		}
		var classType = (ClassDescriptor) operand;
		push(newObject(classType));
	}

	private Pointer newObject(ClassDescriptor type) {
		var newObject = heap.allocateObject(type);
		var fields = type.getAllFields();
		for (int index = 0; index < fields.length; index++) {
			heap.writeField(newObject, index, defaultValue(fields[index].getType()));
		}
		return newObject;
	}

	private void invokeVirtual(Object operand) {
		if (!(operand instanceof MethodDescriptor)) {
			throw new InvalidBytecodeException("invokevirtual has no method operand");
		}
		var staticMethod = (MethodDescriptor) operand;
		var parameterTypes = staticMethod.getParameterTypes();
		var arguments = new Object[parameterTypes.length];
		for (int index = arguments.length - 1; index >= 0; index--) {
			arguments[index] = pop();
			checkType(arguments[index], parameterTypes[index]);
		}
		var target = checkPointer(pop());
		invokeVirtual(staticMethod, target, arguments);
	}

	private void invokeVirtual(MethodDescriptor staticMethod, Pointer target, Object[] arguments) {
		if (target == null) {
			throw new VMException("Null dereferenced");
		}
		@SuppressWarnings("unused")
		var type = getClassDescriptor(target);
		// TODO: Homework Week 7: Implement
		//var dynamicMethod = staticMethod; // TODO: Correct this line with homework week 7
		int postition=staticMethod.getPosition();
		var dynamicMethod=type.getVirtualTable()[postition];
		var locals = initLocals(staticMethod.getLocalTypes());
		if (useJIT && JITPrecondition.fulfilled(dynamicMethod)) {
			performJITCall(dynamicMethod, arguments);
		} else {
			callStack.push(new ActivationFrame(dynamicMethod, target, arguments, locals));
		}
	}

	private void performJITCall(MethodDescriptor dynamicMethod, Object[] arguments) {
		var code = new JITCompiler(dynamicMethod).getCode();
		var result = JITNative.call(code, arguments, dynamicMethod.getReturnType());
		if (dynamicMethod.getReturnType() != null) {
			push(result);
		}
	}

	private Object[] initLocals(TypeDescriptor[] localTypes) {
		var variables = new Object[localTypes.length];
		for (int index = 0; index < localTypes.length; index++) {
			variables[index] = defaultValue(localTypes[index]);
		}
		return variables;
	}

	private void invokeStatic(Object operand) {
		if (operand == MethodDescriptor.HALT_METHOD) {
			var message = checkString(pop());
			throw new VMException("HALT: " + message);
		} else if (operand == MethodDescriptor.WRITE_INT_METHOD) {
			var value = checkInt(pop());
			System.out.print(value);
		} else if (operand == MethodDescriptor.WRITE_STRING_METHOD) {
			var value = checkString(pop());
			System.out.print(value + "\r\n");
		} else if (operand == MethodDescriptor.READ_INT_METHOD) {
			push(input.nextInt());
			input.nextLine();
		} else if (operand == MethodDescriptor.READ_STRING_METHOD) {
			push(input.nextLine());
		} else {
			throw new InvalidBytecodeException("invokestatic for undefined inbuilt method");
		}
	}

	private ClassDescriptor getClassDescriptor(Pointer instance) {
		var descriptor = heap.getDescriptor(instance);
		if (!(descriptor instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("Type mismatch");
		}
		return (ClassDescriptor) descriptor;
	}

	private Object defaultValue(TypeDescriptor type) {
		if (type == TypeDescriptor.BOOLEAN_TYPE) {
			return false;
		} else if (type == TypeDescriptor.INT_TYPE) {
			return 0;
		} else {
			return null;
		}
	}

	private void checkType(Object value, TypeDescriptor type) {
		if (type == TypeDescriptor.BOOLEAN_TYPE) {
			checkBoolean(value);
		} else if (type == TypeDescriptor.INT_TYPE) {
			checkInt(value);
		} else if (type == TypeDescriptor.STRING_TYPE) {
			checkString(value);
		} else if (!typeTest(checkPointer(value), type)) {
			throw new InvalidBytecodeException("Type mismatch");
		}
	}

	@SuppressWarnings("unused")
	private boolean typeTest(Pointer instance, TypeDescriptor targetType) {
		if (instance == null) {
			return true;
		}
		var sourceType = heap.getDescriptor(instance);
		if (sourceType == targetType) {
			return true;
		}
		if (!(sourceType instanceof ClassDescriptor && targetType instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("Type mismatch");
		}
		@SuppressWarnings("unused")
		var sourceClass = (ClassDescriptor) sourceType;
		var targetClass = (ClassDescriptor) targetType;
		// TODO: Homework Week 7: Implement
		throw new RuntimeException("not yet implemented");
	}

	private boolean checkBoolean(Object value) {
		if (!(value instanceof Boolean)) {
			throw new InvalidBytecodeException("Expected boolean instead of " + value);
		}
		return (boolean) value;
	}

	private int checkInt(Object value) {
		if (!(value instanceof Integer)) {
			throw new InvalidBytecodeException("Expected int instead of " + value);
		}
		return (int) value;
	}

	private String checkString(Object value) {
		if (value != null && !(value instanceof String)) {
			throw new InvalidBytecodeException("Expected string instead of " + value);
		}
		return (String) value;
	}
	
	private Pointer checkPointer(Object value) {
		if (value != null && !(value instanceof Pointer)) {
			throw new InvalidBytecodeException("Expected pointer instead of " + value);
		}
		return (Pointer) value;
	}

	private ActivationFrame activeFrame() {
		return callStack.peek();
	}

	private void push(Object value) {
		activeFrame().getEvaluationStack().push(value);
	}

	private Object pop() {
		return activeFrame().getEvaluationStack().pop();
	}
}
