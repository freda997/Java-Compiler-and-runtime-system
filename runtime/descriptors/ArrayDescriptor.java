package runtime.descriptors;

public class ArrayDescriptor extends TypeDescriptor {
	private TypeDescriptor elementType;

	public ArrayDescriptor(String identifier) {
		super(identifier);
	}

	public TypeDescriptor getElementType() {
		return elementType;
	}

	public void setElementType(TypeDescriptor elementType) {
		this.elementType = elementType;
	}
}
