package jit.x64;

import java.util.ArrayList;
import java.util.List;

public class X64Label {
	private final List<X64Jump> jumps = new ArrayList<>();
	private Integer target;

	public List<X64Jump> getJumps() {
		return jumps;
	}

	public Integer getTarget() {
		return target;
	}

	public void setTarget(Integer target) {
		this.target = target;
	}
}