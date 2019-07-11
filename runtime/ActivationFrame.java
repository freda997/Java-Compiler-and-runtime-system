package runtime;

import runtime.descriptors.MethodDescriptor;
import runtime.heap.Pointer;

public class ActivationFrame {
	private final MethodDescriptor method;
	private final Pointer thisReference;
	private final Object[] parameters;
	private final Object[] locals;

	private final EvaluationStack evaluationStack = new EvaluationStack();
	private int instructionPointer;

	public ActivationFrame(MethodDescriptor method, Pointer thisReference, Object[] parameters, Object[] locals) {
		this.method = method;
		this.thisReference = thisReference;
		this.parameters = parameters;
		this.locals = locals;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public Object[] getLocals() {
		return locals;
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public Pointer getThisReference() {
		return thisReference;
	}

	public EvaluationStack getEvaluationStack() {
		return evaluationStack;
	}
	
	public int getInstructionPointer() {
		return instructionPointer;
	}

	public void setInstructionPointer(int instructionPointer) {
		this.instructionPointer = instructionPointer;
	}
}
