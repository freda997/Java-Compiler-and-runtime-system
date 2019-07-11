package runtime.heap;

import java.util.HashMap;
import java.util.Map;

public class DualMap<L, R> {
	private final Map<L, R> leftToRight = new HashMap<>();
	private final Map<R, L> rightToLeft = new HashMap<>();
	
	public void put(L left, R right) {
		if (leftToRight.containsKey(left) || rightToLeft.containsKey(right)) {
			throw new IllegalArgumentException("Already assigned");
		}
		leftToRight.put(left, right);
		rightToLeft.put(right, left);
	}
	
	public boolean containsRight(R right) {
		return rightToLeft.containsKey(right);
	}
	
	public boolean containsLeft(L left) {
		return leftToRight.containsKey(left);
	}
	
	public R getRight(L left) {
		return leftToRight.get(left);
	}
	
	public L getLeft(R right) {
		return rightToLeft.get(right);
	}
	
	public int size() {
		return leftToRight.size();
	}
}
