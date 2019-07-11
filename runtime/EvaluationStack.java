package runtime;

import java.util.Deque;
import java.util.LinkedList;
import error.InvalidBytecodeException;

public class EvaluationStack {
	private static final int LIMIT = 100;

	private final Deque<Object> stack = new LinkedList<>();

	public void push(Object value) {
		if (stack.size() == LIMIT) {
			throw new InvalidBytecodeException("Evaluation stack overflow");
		}
		stack.push(value);
	}

	public Object pop() {
		if (stack.size() == 0) {
			throw new InvalidBytecodeException("Evaluation stack underflow");
		}
		return stack.pop();
	}
	
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	public Object[] toArray() {
		return stack.toArray();
	}
}
