package runtime.heap;

import com.sun.jna.Memory;

import error.InvalidBytecodeException;
import error.VMException;
import runtime.CallStack;
import runtime.descriptors.ArrayDescriptor;
import runtime.descriptors.ClassDescriptor;
import runtime.descriptors.TypeDescriptor;

public class Heap {
	public static final long NULL_POINTER = 0;
	private static final int HEAP_SIZE = 8 * 1024;
	static final int BLOCK_HEADER_SIZE = 16;
	private static final int TAG_OFFSET = 8;
	private static final int POINTER_SIZE = 8;
	static final int HEAP_START = 8;
	static final int HEAP_END = HEAP_SIZE;
	private final Memory heap = new Memory(HEAP_SIZE);
	private final DualMap<TypeDescriptor, Integer> typeDescriptors = new DualMap<>();
	private final DualMap<String, Integer> stringPool = new DualMap<>();
	private final FreeList freeList = new FreeList();
	private final GC gc;

	public Heap(CallStack stack) {
		setBlockSize(HEAP_START, HEAP_SIZE - HEAP_START);
		freeList.add(HEAP_START);
		gc = new GC(this, freeList, stack);
	}

	public TypeDescriptor getDescriptor(Pointer instance) {
		var link = (int) readLong64(getAddress(instance) - TAG_OFFSET);
		return typeDescriptors.getLeft(link);
	}

	public Pointer allocateObject(ClassDescriptor type) {
		return allocate(type.getAllFields().length * POINTER_SIZE, type);
	}

	public Object readField(Pointer instance, int index) {
		var nativeValue = readLong64(getAddress(instance) + index * POINTER_SIZE);
		var descriptor = getDescriptor(instance);
		if (!(descriptor instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("Invalid field read");
		}
		var classType = (ClassDescriptor) descriptor;
		var fieldType = classType.getAllFields()[index].getType();
		return fromNativeValue(fieldType, nativeValue);
	}

	public void writeField(Pointer instance, int index, Object value) {
		var descriptor = getDescriptor(instance);
		if (!(descriptor instanceof ClassDescriptor)) {
			throw new InvalidBytecodeException("Invalid field read");
		}
		var classType = (ClassDescriptor) descriptor;
		var fieldType = classType.getAllFields()[index].getType();
		var nativeValue = toNativeValue(fieldType, value);
		writeLong64(getAddress(instance) + index * POINTER_SIZE, nativeValue);
	}

	public Pointer allocateArray(ArrayDescriptor type, int length) {
		return allocate(length * POINTER_SIZE, type);
	}

	public int getArrayLength(Pointer array) {
		var blockSize = getBlockSize(getAddress(array) - BLOCK_HEADER_SIZE);
		return (blockSize - BLOCK_HEADER_SIZE) / POINTER_SIZE;
	}

	public Object readElement(Pointer array, int index) {
		var nativeValue = readLong64(getAddress(array) + index * POINTER_SIZE);
		var descriptor = getDescriptor(array);
		if (!(descriptor instanceof ArrayDescriptor)) {
			throw new InvalidBytecodeException("Invalid array read");
		}
		var arrayType = (ArrayDescriptor) descriptor;
		return fromNativeValue(arrayType.getElementType(), nativeValue);
	}

	public void writeElement(Pointer array, int index, Object value) {
		var descriptor = getDescriptor(array);
		if (!(descriptor instanceof ArrayDescriptor)) {
			throw new InvalidBytecodeException("Invalid array read");
		}
		var arrayType = (ArrayDescriptor) descriptor;
		var nativeValue = toNativeValue(arrayType.getElementType(), value);
		writeLong64(getAddress(array) + index * POINTER_SIZE, nativeValue);
	}

	private Pointer allocate(int size, TypeDescriptor type) {
		if (size < 0) {
			throw new IllegalArgumentException("Negative block length");
		}
		var grossSize = size + BLOCK_HEADER_SIZE;
		var newBlock = freeList.removeNewBlock(grossSize, this::getBlockSize);
		if (newBlock == NULL_POINTER) {
			gc.collect();
			newBlock = freeList.removeNewBlock(grossSize, this::getBlockSize);
			if (newBlock == NULL_POINTER) {
				throw new VMException("Out of memory");
			}
		}
		if (getBlockSize(newBlock) > grossSize) {
			var unusedBlock = newBlock + grossSize;
			setBlockSize(unusedBlock, getBlockSize(newBlock) - grossSize);
			freeList.add(unusedBlock);
		}
		setBlockSize(newBlock, grossSize);
		setTypeDescriptor(type, newBlock);
		return getPointer(newBlock + BLOCK_HEADER_SIZE);
	}

	private void setTypeDescriptor(TypeDescriptor type, long blockAddress) {
		if (!typeDescriptors.containsLeft(type)) {
			typeDescriptors.put(type, typeDescriptors.size());
		}
		var link = typeDescriptors.getRight(type);
		writeLong64(blockAddress + TAG_OFFSET, link);
	}

	int getBlockSize(long blockAddress) {
		return (int) (readLong64(blockAddress) & 0x7fffffff_ffffffffL);
	}

	void setBlockSize(long address, int size) {
		writeLong64(address, size);
	}

	void writeLong64(long address, long value) {
		heap.setLong(address, value);
	}

	long readLong64(long address) {
		return heap.getLong(address);
	}

	private Object fromNativeValue(TypeDescriptor type, long value) {
		if (type == TypeDescriptor.BOOLEAN_TYPE) {
			return value == 1;
		}
		if (type == TypeDescriptor.INT_TYPE) {
			return (int) value;
		}
		if (type == TypeDescriptor.STRING_TYPE) {
			return stringPool.getLeft((int) value);
		}
		if (value == NULL_POINTER) {
			return null;
		}
		return getPointer(value);
	}

	private long toNativeValue(TypeDescriptor type, Object value) {
		if (type == TypeDescriptor.BOOLEAN_TYPE) {
			return (boolean) value ? 1 : 0;
		}
		if (type == TypeDescriptor.INT_TYPE) {
			return (int) value;
		}
		if (type == TypeDescriptor.STRING_TYPE) {
			return allocateString((String) value);
		}
		if (value == null) {
			return NULL_POINTER;
		}
		return getAddress((Pointer)value);
	}

	long getAddress(Pointer pointer) {
		return pointer.getAddress();
	}

	Pointer getPointer(long address) {
		return new Pointer(address);
	}

	private long allocateString(String value) {
		if (!stringPool.containsLeft(value)) {
			stringPool.put(value, stringPool.size());
		}
		return stringPool.getRight(value);
	}
}
