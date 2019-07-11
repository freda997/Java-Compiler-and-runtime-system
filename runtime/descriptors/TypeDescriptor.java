package runtime.descriptors;

public class TypeDescriptor {
	public static final TypeDescriptor BOOLEAN_TYPE = new TypeDescriptor("boolean");
	public static final TypeDescriptor INT_TYPE = new TypeDescriptor("int");
	public static final TypeDescriptor STRING_TYPE = new TypeDescriptor("string");

	private final String identifier;
	
	public TypeDescriptor(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}
	
	@Override
	public String toString() {
		return identifier;
	}
}
