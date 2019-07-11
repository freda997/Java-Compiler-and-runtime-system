package jit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sun.jna.Platform;

import bytecode.Instruction;
import bytecode.OpCode;
import jit.x64.X64Assembler;
import jit.x64.X64Label;
import jit.x64.X64Register;
import runtime.descriptors.MethodDescriptor;
import static jit.x64.X64Register.*;
import static bytecode.OpCode.*;

public class JITCompiler {
	private static final List<X64Register> GENERAL_REGISTERS = List.of(RAX, RCX, RDX, RBX, R8, R9, R10, R11, R12, R13,
			R14, R15);
	private static final List<X64Register> CALLEE_SAVED = List.of(RBX, R12, R13, R14, R15);

	private static final Set<OpCode> BRANCH_INSTRUCTIONS = Set.of(GOTO, IF_TRUE, IF_FALSE);
	private static final Set<OpCode> UNCONDITIONAL_BRANCHES = Set.of(GOTO, RETURN);

	private final MethodDescriptor method;
	private final X64Assembler assembler = new X64Assembler();
	private final Allocation allocation;
	private final Map<X64Label, Allocation> branchState = new HashMap<>();
	private final Map<Instruction, X64Label> labels = new HashMap<>();

	public JITCompiler(MethodDescriptor method) {
		Objects.requireNonNull(method);
		this.method = method;
		if (!JITPrecondition.fulfilled(method)) {
			throw new AssertionError("Preconditions for JIT compilation not fulfilled");
		}
		var parameters = allocateParameters(method.getParameterTypes().length);
		var free = new HashSet<X64Register>(GENERAL_REGISTERS);
		free.removeAll(parameters);
		allocation = new Allocation(parameters, free);
		allocateLocals(method.getLocalTypes().length);
		createLabels(method.getCode());
		emitInstructions(method.getCode());
	}

	public byte[] getCode() {
		return assembler.getCode();
	}

	private void createLabels(Instruction[] instructions) {
		for (int position = 0; position < instructions.length; position++) {
			var current = instructions[position];
			if (BRANCH_INSTRUCTIONS.contains(current.getOpCode())) {
				var target = instructions[position + 1 + (int) current.getOperand()];
				if (!labels.containsKey(target)) {
					labels.put(target, assembler.createLabel());
				}
			}
		}
	}

	private void emitInstructions(Instruction[] code) {
		emitPrologue();
		for (int position = 0; position < code.length; position++) {
			alignBranchEntry(position);
			emitInstruction(position);
		}
	}

	private void emitInstruction(int position) {
		var code = method.getCode();
		var instruction = code[position];
		var opCode = instruction.getOpCode();
		var operand = instruction.getOperand();
		X64Register operand2;
		X64Register operand1;
		X64Register result_reg;
		X64Label label;
		switch (opCode) {
		case LDC:
			emitConstant(operand);
			break;
		case BNEG:
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.NOT(result_reg);
			release(operand1);
			push(result_reg);
			break;
		case INEG:
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.NEG(result_reg);
			release(operand1);
			push(result_reg);
			break;
		case IADD:
			operand2=pop();  //get operand register from evaluation stack
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.ADD_RegReg(result_reg,operand2); //result=leftoperand-rightoperand
			release(operand1);
			release(operand2);
			push(result_reg);
			break;
		case ISUB:
			operand2=pop();  //get operand register from evaluation stack
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.SUB_RegReg(result_reg,operand2); //result=leftoperand-rightoperand
			release(operand1);
			release(operand2);
			push(result_reg);
			break;
		case IMUL:
			operand2=pop();  //get operand register from evaluation stack
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.IMUL_RegReg(result_reg,operand2); //result=leftoperand-rightoperand
			release(operand1);
			release(operand2);
			push(result_reg);
			break;
		case IDIV:
			reserve(RAX);
			reserve(RDX); //free rax and rdx
			forceStack(1,RAX); //make sure stack pos 1 is in RAX (dividend in this case)
			var operand_2_div=pop();
			pop(); //forcestack makesure this go to RAX
			assembler.CDQ(); //prepare RAX
			assembler.IDIV(operand_2_div);
			push(RAX);
			release(operand_2_div);
			release(RDX);

			break;
		case IREM:
			// TODO: Implement
			reserve(RAX);
			reserve(RDX); //free rax and rdx
			forceStack(1,RAX); //make sure stack pos 1 is in RAX (dividend in this case)
			var operand_2_rem=pop();
			pop(); //forcestack makesure this go to RAX
			assembler.CDQ(); //prepare RAX
			assembler.IDIV(operand_2_rem);
			push(RDX);  //get remainder
			release(operand_2_rem);
			release(RAX);

			break;
		case LOAD:
			// TODO: Implement
			var index =(int) instruction.getOperand();
			int numOfParams=allocation.getParameters().size();
			int numOfLocals=allocation.getLocals().size();
			if (index<=numOfParams){
				var reg=allocation.getParameters().get(index-1);
				push(reg);
			}
			else if (index-numOfParams<=numOfLocals){
				var reg=allocation.getLocals().get(index-numOfParams-1);
				push(reg);
			}
			else {
				throw new AssertionError("Can not find the variable");
			}
			break;
		case STORE:
			var index2 =(int) instruction.getOperand();
			var source=pop();
			int numOfParams2=allocation.getParameters().size();
			int numOfLocals2=allocation.getLocals().size();
			if (index2<=numOfParams2){
				var reg=allocation.getParameters().get(index2-1);
				assembler.MOV_RegReg(reg,source);

			}
			else if (index2-numOfParams2<=numOfLocals2){
				var reg=allocation.getLocals().get(index2-numOfParams2-1);
				assembler.MOV_RegReg(reg,source);
			}
			else {
				throw new AssertionError("Can not find the variable");
			}
			release(source);
			break;
		case CMPEQ:
			// TODO: Implement

			//break;
		case CMPNE:
			// TODO: Implement
			//break;
		case ICMPLT:
			// TODO: Implement
			//break;
		case ICMPLE:
			// TODO: Implement
			//break;
		case ICMPGT:
			// TODO: Implement
			//break;
		case ICMPGE:
			// TODO: Implement
			operand2=pop();  //get operand register from evaluation stack
			operand1=pop();
			result_reg=acquire();
			assembler.MOV_RegReg(result_reg,operand1); //result=leftoperand
			assembler.CMP_RegReg(result_reg,operand2); //result=leftoperand-rightoperand
			release(operand1);
			release(operand2);
			release(result_reg);
			//push(result_reg);
			break;
		case IF_FALSE:
			// TODO: Implement
			var offset=(int) instruction.getOperand();
			var target=code[position+1+offset];
			label=labels.get(target);
			matchAllocation(label);
			var previous = position > 0 ? code[position - 1] : null;
			if (previous==null){
				throw new AssertionError("no boolean recorded");
			}
			var prev_opcode=previous.getOpCode();
			switch (prev_opcode) {
				case ICMPGT:
					assembler.JLE_Rel(label);
					break;
				case CMPEQ:
					assembler.JNE_Rel(label);
					break;
				case CMPNE:
					assembler.JE_Rel(label);
					break;
				case ICMPLT:
					assembler.JGE_Rel(label);
					break;
				case ICMPGE:
					assembler.JL_Rel(label);
					break;
				case ICMPLE:
					assembler.JG_Rel(label);
					break;
				default:
					throw new AssertionError("Unsupported instruction in JIT compiler");

			}

			break;
		case IF_TRUE:
			// TODO: Implement
			var offset2=(int) instruction.getOperand();
			var target2=code[position+1+offset2];
			label=labels.get(target2);
			matchAllocation(label);
			var previous2 = position > 0 ? code[position - 1] : null;
			if (previous2==null){
				throw new AssertionError("no boolean recorded");
			}
			var prev_opcode2=previous2.getOpCode();
			switch (prev_opcode2){
				case CMPEQ:
					assembler.JE_Rel(label);
					break;
				case CMPNE:
					assembler.JNE_Rel(label);
					break;
				case ICMPLT:
					assembler.JL_Rel(label);
					break;
				case ICMPGT:
					assembler.JG_Rel(label);
					break;
				case ICMPGE:
					assembler.JGE_Rel(label);
					break;
				case ICMPLE:
					assembler.JLE_Rel(label);
					break;

				default:
					throw new AssertionError("Unsupported instruction in JIT compiler");
			}

			break;
		case GOTO:
			// TODO: Implement
			var offset3=(int) instruction.getOperand();
			var target3=code[position+1+offset3];
			label=labels.get(target3);
			matchAllocation(label);
			assembler.JMP_Rel(label);
			break;
		case RETURN:
			emitReturn();
			break;
		default:
			throw new AssertionError("Unsupported instruction in JIT compiler");
		}
	}

	private void emitReturn() {
		if (method.getReturnType() != null && allocation.getEvaluation().size() == 1) {
			forceStack(0, RAX);
			if (allocation.getEvaluation().pop() != RAX) {
				throw new AssertionError("Return must be in RAX");
			}
			release(RAX);
		}
		if (allocation.getEvaluation().size() != 0) {
			throw new AssertionError("Register stack not empty");
		}
		emitEpilogue();
	}

	private void emitConstant(Object operand) {
		int value;
		if (operand instanceof Integer) {
			value = (int) operand;
		} else if (operand instanceof Boolean) {
			value = (boolean) operand ? 1 : 0;
		} else {
			throw new AssertionError("Unsupported LDC operand in JIT compiler");
		}
		var target = acquire();
		assembler.MOV_RegImm(target, value);  //mov rax,value
		push(target);
	}

	private void emitPrologue() {
		for (var register : CALLEE_SAVED) {
			assembler.PUSH(register);
		}
	}

	private void emitEpilogue() {
		var list = new ArrayList<>(CALLEE_SAVED);
		Collections.reverse(list);
		for (var register : list) {
			assembler.POP(register);
		}
		assembler.RET();
	}

	private void alignBranchEntry(int position) {
		var code = method.getCode();
		var current = code[position];
		if (labels.containsKey(current)) {
			var label = labels.get(current);
			assembler.setLabel(label);
			var previous = position > 0 ? code[position - 1] : null;
			if (previous == null || !UNCONDITIONAL_BRANCHES.contains(previous.getOpCode())) {
				matchAllocation(label);
			} else {
				resetAllocation(label);
			}
		}
	}

	private void matchAllocation(X64Label label) {
		if (!branchState.containsKey(label)) {
			branchState.put(label, allocation.clone());
		} else {
			realignAllocation(branchState.get(label));
		}
	}

	private void resetAllocation(X64Label label) {
		if (branchState.containsKey(label)) {
			var expected = branchState.get(label);
			expected.copyTo(allocation);
		}
	}

	private void realignAllocation(Allocation expected) {
		realignRegisters(allocation.getParameters(), expected.getParameters());
		realignRegisters(allocation.getLocals(), expected.getLocals());
		realignRegisters(allocation.getEvaluation(), expected.getEvaluation());
		if (!allocation.equals(expected)) {
			throw new AssertionError("Failed allocation alignment");
		}
		expected.copyTo(allocation);
	}

	private void realignRegisters(List<X64Register> actual, List<X64Register> expected) {
		if (actual.size() != expected.size()) {
			throw new AssertionError("Inconsistent number of registers");
		}
		for (int index = 0; index < actual.size(); index++) {
			var source = actual.get(index);
			var target = expected.get(index);
			if (source != target) {
				reserve(target);
				forceRegister(actual, index, target);
				release(source);
			}
		}
	}

	private void forceStack(int stackPos, X64Register reg) {
		forceRegister(allocation.getEvaluation(), stackPos, reg);
	}

	private void forceRegister(List<X64Register> list, int index, X64Register reg) {
		var current = list.get(index);
		if (current != reg) {
			assembler.MOV_RegReg(reg, current);
			list.set(index, reg);
		}
	}

	private X64Register acquire() {
		return allocation.acquire();
	}

	private void release(X64Register reg) {
		allocation.release(reg);
	}

	private void reserve(X64Register reg) {
		if (allocation.isFree(reg)) {
			allocation.acquireSpecific(reg);
		} else {
			var target = allocation.acquire();
			assembler.MOV_RegReg(target, reg);
			allocation.relocate(reg, target);  //relocation: tranfer all metadata about reg into a new reg to free up reg
		}
	}

	private void push(X64Register register) {
		allocation.getEvaluation().push(register);
	}

	@SuppressWarnings("unused")
	private X64Register pop() {
		return allocation.getEvaluation().pop();
	}

	@SuppressWarnings("unused")
	private X64Register peek() {
		return allocation.getEvaluation().peek();
	}

	private List<X64Register> allocateParameters(int paramCount) {
		boolean isWindows = Platform.isWindows();
		var parameters = new ArrayList<X64Register>();
		if (paramCount > 0) {
			parameters.add(isWindows ? RCX : RDI);
		}
		if (paramCount > 1) {
			parameters.add(isWindows ? RDX : RSI);
		}
		if (paramCount > 2) {
			parameters.add(isWindows ? R8 : RDX);
		}
		if (paramCount > 3) {
			parameters.add(isWindows ? R9 : RCX);
		}
		if (paramCount > 4) {
			throw new AssertionError("Only four parameters currently supported");
		}
		return parameters;
	}

	private void allocateLocals(int localCount) {
		for (int index = 0; index < localCount; index++) {
			allocation.addLocal();
		}
	}
}
