package runtime.descriptors;

public class ClassDescriptor extends TypeDescriptor {
	private FieldDescriptor[] allFields;
	private ClassDescriptor[] ancestorTable;
	private MethodDescriptor[] virtualTable;

	public ClassDescriptor(String identifier) {
		super(identifier);
	}

	public FieldDescriptor[] getAllFields() {
		return allFields;
	}

	public void setAllFields(FieldDescriptor[] allFields) {
		this.allFields = allFields;
	}

	public int getAncestorLevel() {
		return ancestorTable.length - 1;
	}

	public ClassDescriptor[] getAncestorTable() {
		return ancestorTable;
	}

	public void setAncestorTable(ClassDescriptor[] ancestorTable) {
		this.ancestorTable = ancestorTable;
	}

	public MethodDescriptor[] getVirtualTable() {
		return virtualTable;
	}

	public void setVirtualTable(MethodDescriptor[] virtualTable) {
		this.virtualTable = virtualTable;
	}
}
