package runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import bytecode.BytecodeArray;
import bytecode.BytecodeAssembly;
import bytecode.BytecodeClass;
import bytecode.BytecodeField;
import bytecode.BytecodeMethod;
import bytecode.BytecodeType;
import bytecode.Instruction;
import error.InvalidBytecodeException;
import runtime.descriptors.ArrayDescriptor;
import runtime.descriptors.ClassDescriptor;
import runtime.descriptors.FieldDescriptor;
import runtime.descriptors.MethodDescriptor;
import runtime.descriptors.TypeDescriptor;

public class Loader {
	private final BytecodeAssembly assembly;
	private final Map<BytecodeType, TypeDescriptor> typeMap = new HashMap<>();
	private final Map<BytecodeMethod, MethodDescriptor> methodMap = new HashMap<>();
	private final Map<BytecodeField, FieldDescriptor> fieldMap = new HashMap<>();

	public Loader(BytecodeAssembly assembly) {
		Objects.requireNonNull(assembly);
		this.assembly = assembly;
		registerInbuilts();
		createDescriptors();
		patchCode();
	}
	
	public MethodDescriptor getMainMethod() {
		return getMethodDescriptor(assembly.getMainMethod());
	}
	
	public ClassDescriptor getMainClass() {
		return getClassDescriptor(assembly.getMainMethod().getContainingClass());
	}

	private void patchCode() {
		for (var method : methodMap.keySet()) {
			if (method.getCode() != null) {
				var descriptor = methodMap.get(method);
				descriptor.setCode(patchCode(method.getCode()));
			}
		}
	}

	private Instruction[] patchCode(List<Instruction> code) {
		var result = new Instruction[code.size()];
		var index = 0;
		for (var instruction : code) {
			var operand = instruction.getOperand();
			if (operand instanceof BytecodeType) {
				operand = getTypeDescriptor((BytecodeType)operand);
			} else if (operand instanceof BytecodeMethod) {
				operand = getMethodDescriptor((BytecodeMethod)operand);
			} else if (operand instanceof BytecodeField) {
				operand = getFieldDescriptor((BytecodeField)operand);
			}
			result[index] = new Instruction(instruction.getOpCode(), operand);
			index++;
		}
		return result;
	}

	private void registerInbuilts() {
		typeMap.put(assembly.getBooleanType(), TypeDescriptor.BOOLEAN_TYPE);
		typeMap.put(assembly.getIntType(), TypeDescriptor.INT_TYPE);
		typeMap.put(assembly.getStringType(), TypeDescriptor.STRING_TYPE);
		methodMap.put(assembly.getHaltMethod(), MethodDescriptor.HALT_METHOD);
		methodMap.put(assembly.getWriteIntMethod(), MethodDescriptor.WRITE_INT_METHOD);
		methodMap.put(assembly.getWriteStringMethod(), MethodDescriptor.WRITE_STRING_METHOD);
		methodMap.put(assembly.getReadIntMethod(), MethodDescriptor.READ_INT_METHOD);
		methodMap.put(assembly.getReadStringMethod(), MethodDescriptor.READ_STRING_METHOD);
	}

	private TypeDescriptor getTypeDescriptor(BytecodeType type) {
		if (type instanceof BytecodeClass) {
			return getClassDescriptor((BytecodeClass) type);
		} else if (type instanceof BytecodeArray) {
			return getArrayDescriptor((BytecodeArray) type);
		} else {
			return typeMap.get(type);
		}
	}

	private ClassDescriptor getClassDescriptor(BytecodeClass type) {
		if (!typeMap.containsKey(type)) {
			var descriptor = new ClassDescriptor(type.getIdentifier());
			typeMap.put(type, descriptor);
			checkAcyclicInheritance(type);
			descriptor.setAllFields(collectFieldTypes(type));
			descriptor.setAncestorTable(buildAncestorTable(type));
			descriptor.setVirtualTable(buildVirtualTable(type));
		}
		return (ClassDescriptor) typeMap.get(type);
	}

	private ArrayDescriptor getArrayDescriptor(BytecodeArray type) {
		if (!typeMap.containsKey(type)) {
			var descriptor = new ArrayDescriptor(type.getIdentifier());
			typeMap.put(type, descriptor);
			descriptor.setElementType(getTypeDescriptor(type.getElementType()));
		}
		return (ArrayDescriptor) typeMap.get(type);
	}

	private MethodDescriptor getMethodDescriptor(BytecodeMethod method) {
		if (!methodMap.containsKey(method)) {
			var descriptor = new MethodDescriptor(method.getIdentifier());
			methodMap.put(method, descriptor);
			descriptor.setReturnType(getTypeDescriptor(method.getReturnType()));
			descriptor.setParameterTypes(getTypeDescriptors(method.getParameterTypes()));
			descriptor.setLocalTypes(getTypeDescriptors(method.getLocalTypes()));
		}
		return methodMap.get(method);
	}

	private FieldDescriptor getFieldDescriptor(BytecodeField field) {
		if (!fieldMap.containsKey(field)) {
			var descriptor = new FieldDescriptor(field.getIdentifier());
			fieldMap.put(field, descriptor);
			descriptor.setType(getTypeDescriptor(field.getType()));
		}
		return fieldMap.get(field);
	}

	private void createDescriptors() {
		for (var type : assembly.getTypes()) {
			getTypeDescriptor(type);
		}
	}

	private void checkAcyclicInheritance(BytecodeClass classType) {
		var visited = new HashSet<BytecodeClass>();
		while (classType != null) {
			if (visited.contains(classType)) {
				throw new InvalidBytecodeException("Cyclic inheritance: " + classType);
			}
			visited.add(classType);
			classType = classType.getBaseType();
		}
	}

	private FieldDescriptor[] collectFieldTypes(BytecodeClass classType) {
		var list = new ArrayList<FieldDescriptor>();
		var baseClass = classType.getBaseType();
		if (baseClass != null) {
			var baseDescriptor = getClassDescriptor(baseClass);
			list.addAll(List.of(baseDescriptor.getAllFields()));
		}
		for (var field : classType.getFields()) {
			var fieldDescriptor = getFieldDescriptor(field);
			fieldDescriptor.setIndex(list.size());
			list.add(fieldDescriptor);
		}
		return list.toArray(new FieldDescriptor[0]);
	}

	private ClassDescriptor[] buildAncestorTable(BytecodeClass classType) {
		var list = new ArrayList<ClassDescriptor>();
		// TODO: Homework Week 7: Extend
		var baseType=classType.getBaseType();
		if (baseType!=null) {
			var baseDesc = getClassDescriptor(baseType);
			var baseAnces = baseDesc.getAncestorTable();
			list.addAll(List.of(baseAnces.clone()));
		}
		list.add(getClassDescriptor(classType));
		return list.toArray(new ClassDescriptor[0]);
	}

	private MethodDescriptor[] buildVirtualTable(BytecodeClass classType) {
		var result = new ArrayList<MethodDescriptor>();
		// TODO: Homework Week 7: Extend
		//var baseMethods=getMethodDescriptor(classType);
		var baseType = classType.getBaseType();
		boolean overidden = false;
		if (baseType != null) {
			var baseDesc = getClassDescriptor(classType.getBaseType());
			var baseVirtual = baseDesc.getVirtualTable();
			result.addAll(List.of(baseVirtual.clone()));
		}
		for (var method : classType.getMethods()) {
			var methodDescriptor = getMethodDescriptor(method);
			overidden = false;
			for (var m : result) {
				if (m.getIdentifier().equals(methodDescriptor.getIdentifier())) {

						//if (m.getParameterTypes() == methodDescriptor.getParameterTypes()) {
							result.set(m.getPosition(), methodDescriptor);
							methodDescriptor.setPosition(m.getPosition());
							overidden = true;
							break;
						//}

				}
			}

			if (!overidden) {
				methodDescriptor.setPosition(result.size());
				result.add(methodDescriptor);

			}

		}
		return result.toArray(new MethodDescriptor[0]);

	}

	@SuppressWarnings("unused")
	private void checkOverriding(MethodDescriptor first, BytecodeMethod second) {
		if (!Arrays.equals(first.getParameterTypes(), getTypeDescriptors(second.getParameterTypes()))
				|| first.getReturnType() != getTypeDescriptor(second.getReturnType())) {
			throw new InvalidBytecodeException("Invalid signatures on overriding: " + first.getIdentifier());
		}
	}

	private TypeDescriptor[] getTypeDescriptors(List<BytecodeType> types) {
		return types.stream().map(this::getTypeDescriptor).toArray(TypeDescriptor[]::new);
	}
}
