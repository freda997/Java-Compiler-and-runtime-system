package jit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jit.x64.X64Register;

final class Allocation {
	private final List<X64Register> parameters;
	private final Set<X64Register> free;
	private final List<X64Register> locals = new ArrayList<>();
	private final LinkedList<X64Register> evaluation = new LinkedList<>();

	public Allocation(List<X64Register> parameters, Set<X64Register> free) {
		this.parameters = new ArrayList<>(parameters);
		this.free = new HashSet<>(free);
		check();
	}

	public List<X64Register> getParameters() {
		return parameters;
	}

	public Set<X64Register> getFree() {
		return free;
	}

	public List<X64Register> getLocals() {
		return locals;
	}

	public LinkedList<X64Register> getEvaluation() {
		return evaluation;
	}

	public Allocation clone() {
		var copy = new Allocation(parameters, free);
		copy.locals.addAll(locals);
		copy.evaluation.addAll(evaluation);
		return copy;
	}

	public void copyTo(Allocation other) {
		other.parameters.clear();
		other.parameters.addAll(parameters);
		other.free.clear();
		other.free.addAll(free);
		other.locals.clear();
		other.locals.addAll(locals);
		other.evaluation.clear();
		other.evaluation.addAll(evaluation);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		var other = (Allocation) obj;
		return parameters.equals(other.parameters) && free.equals(other.free) && locals.equals(other.locals)
				&& evaluation.equals(other.evaluation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameters, free, locals, evaluation);
	}

	private void check() {
		for (var reg : free) {
			if (parameters.contains(reg) || locals.contains(reg)) {
				throw new AssertionError("Reg " + reg + " should not be free");
			}
		}
	}

	public X64Register addLocal() {
		var reg = acquire();
		locals.add(reg);
		return reg;
	}

	public boolean isFree(X64Register reg) {
		return free.contains(reg);
	}

	public X64Register acquire() {
		var reg = free.iterator().next();
		free.remove(reg);
		return reg;
	}

	public void acquireSpecific(X64Register reg) {
		if (!free.contains(reg)) {
			throw new AssertionError("Register " + reg + " cannot be reserved");
		}
		free.remove(reg);
	}

	public void release(X64Register reg) {
		if (evaluation.contains(reg)) {
			throw new AssertionError("Register " + reg + " cannot be released");
		}
		if (!locals.contains(reg) && !parameters.contains(reg)) {
			free.add(reg);
		}
	}

	public void relocate(X64Register from, X64Register to) {
		parameters.replaceAll(current -> current == from ? to : current);
		locals.replaceAll(current -> current == from ? to : current);
		evaluation.replaceAll(current -> current == from ? to : current);
	}
}
