package runtime.descriptors;

public class FieldDescriptor {
	private final String identifier;
	private int index;
	private TypeDescriptor type;

	public FieldDescriptor(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public TypeDescriptor getType() {
		return type;
	}

	public void setType(TypeDescriptor type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return identifier;
	}
}
