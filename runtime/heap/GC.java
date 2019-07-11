package runtime.heap;

import java.util.ArrayList;
import java.util.List;

import runtime.CallStack;
import runtime.descriptors.ArrayDescriptor;
import runtime.descriptors.ClassDescriptor;

public class GC {
	private final Heap heap;
	@SuppressWarnings("unused")
	private final FreeList freeList;
	@SuppressWarnings("unused")
	private final CallStack stack;
	
	public GC(Heap heap, FreeList freeList, CallStack stack) {
		this.heap = heap;
		this.freeList = freeList;
		this.stack = stack;
	}
	Iterable<Pointer> getPointers(Pointer current){
		var list = new ArrayList<Pointer>();
		var object_type = heap.getDescriptor(current);
		if (object_type instanceof ClassDescriptor) {

			var fields = ((ClassDescriptor) object_type).getAllFields();
			int field_size=fields.length;

			for (int i = 0; i < field_size; i++) {
				if (fields[i].getType() instanceof ClassDescriptor ||
						fields[i].getType() instanceof ArrayDescriptor) {
					var value = heap.readField(current, i);
					if (value instanceof Pointer) {
						if (value != null) list.add((Pointer) value);
					}
				}
			}
		} else {
			int array_size = heap.getArrayLength(current);
			for (int i = 0; i < array_size; i++) {
				var value= heap.readElement(current, i);
				if ( value instanceof Pointer){
					if (value!=null) {
						list.add((Pointer) value);
					}
				}
			}

		}
		return list;
	}
	private void traverse(Pointer current) {
		if (current != null && !isMarked(heap.getAddress(current)-16)) {
			setMark(current.getAddress()-16);
			var list=getPointers(current);
			for (var next : list ) {
				traverse(next);
			}
		}
	}
	private void mark(){
		for (var root:getRootSet(stack)){
			traverse(root);
		}
	}
	private void sweep(){
		var current= Heap.HEAP_START;
		while (current< Heap.HEAP_END){
			if (!isMarked(current)){
				if (!freeList.isFree(current))
				{
					freeList.add(current);
				}
			}
			clearMark(current);
			current+=(heap.getBlockSize(current));
		}
	}
	public void collect() {
		// TODO: Homework Week 8: Implement
		//throw new RuntimeException("GC not yet implemented");
		mark();
		sweep();
	}

	
	@SuppressWarnings("unused")
	private Iterable<Pointer> getRootSet(CallStack callStack) {
		var list = new ArrayList<Pointer>();
		for (var frame : callStack) {
			collectPointers(frame.getParameters(), list);
			collectPointers(frame.getLocals(), list);
			collectPointers(frame.getEvaluationStack().toArray(), list);
			list.add(frame.getThisReference());
		}
		return list;
	}
	
	private void collectPointers(Object[] values, List<Pointer> list) {
		for (var value : values) {
			if (value instanceof Pointer) {
				list.add((Pointer) value);
			}
		}
	}

	@SuppressWarnings("unused")
	private void setMark(long block) {
		heap.writeLong64(block, heap.readLong64(block) | 0x8000000000000000L);
	}

	@SuppressWarnings("unused")
	private void clearMark(long block) {
		heap.writeLong64(block, heap.readLong64(block) & 0x7fffffffffffffffL);
	}

	@SuppressWarnings("unused")
	private boolean isMarked(long block) {
		return (heap.readLong64(block) & 0x8000000000000000L) != 0;
	}
}
