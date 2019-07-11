package runtime.heap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongToIntFunction;

public class FreeList {
	private final List<Long> freeBlocks = new ArrayList<>();

	public long removeNewBlock(long size, LongToIntFunction getBlockLength) {
		for (int index = 0; index < freeBlocks.size(); index++) {
			long address = freeBlocks.get(index);
			if (getBlockLength.applyAsInt(address) >= size) {
				freeBlocks.remove(index);
				return address;
			}
		}
		return Heap.NULL_POINTER;
	}

	public void add(long blockAddress) {
		freeBlocks.add(blockAddress);
	}

	public boolean isFree(long blockAddress) {
		return freeBlocks.contains(blockAddress);
	}
}
