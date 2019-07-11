package runtime;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import error.VMException;

public class CallStack implements Iterable<ActivationFrame >{
	private static final int LIMIT = 100_000;
	
	private final Deque<ActivationFrame> stack = new LinkedList<>();
	
	public void push(ActivationFrame frame) {
		if (stack.size() == LIMIT) {
			throw new VMException("Stack overflow");
		}
		stack.push(frame);
	}
	
	public ActivationFrame pop() {
		return stack.pop();
	}
	
	public ActivationFrame peek() {
		return stack.peek();
	}
	
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public Iterator<ActivationFrame> iterator() {
		return stack.iterator();
	}
}
