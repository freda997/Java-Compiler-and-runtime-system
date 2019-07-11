package bytecode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BytecodeAssembly implements Serializable {
	private static final long serialVersionUID = 1L;

	private final List<BytecodeType> types = new ArrayList<>();
	private final List<BytecodeMethod> methods = new ArrayList<>();
	private BytecodeMethod mainMethod;

	private final BytecodeType booleanType = new BytecodeType("boolean");
	private final BytecodeType intType = new BytecodeType("int");
	private final BytecodeType stringType = new BytecodeType("string");

	private final BytecodeMethod haltMethod = new BytecodeMethod(null, "halt");
	private final BytecodeMethod writeIntMethod = new BytecodeMethod(null, "writeInt");
	private final BytecodeMethod writeStringMethod = new BytecodeMethod(null, "writeString");
	private final BytecodeMethod readIntMethod = new BytecodeMethod(null, "readInt");
	private final BytecodeMethod readStringMethod = new BytecodeMethod(null, "readString");

	public static BytecodeAssembly load(String file) throws IOException {
		try (var stream = new ObjectInputStream(new FileInputStream(file))) {
			var entry = stream.readObject();
			if (entry instanceof BytecodeAssembly) {
				return (BytecodeAssembly)entry;
			} else {
				throw new IOException("Wrong file format");
			}
		} catch (ClassNotFoundException e) {
			throw new IOException("Wrong file format");
		} 
	}
	
	public void save(String file) throws IOException {
		try (var stream = new ObjectOutputStream(new FileOutputStream(file))) {
			stream.writeObject(this);
		}
	}
	
	public List<BytecodeType> getTypes() {
		return types;
	}

	public List<BytecodeMethod> getMethods() {
		return methods;
	}

	public BytecodeMethod getMainMethod() {
		return mainMethod;
	}

	public void setMainMethod(BytecodeMethod mainMethod) {
		this.mainMethod = mainMethod;
	}

	public BytecodeType getBooleanType() {
		return booleanType;
	}

	public BytecodeType getIntType() {
		return intType;
	}

	public BytecodeType getStringType() {
		return stringType;
	}

	public BytecodeMethod getHaltMethod() {
		return haltMethod;
	}

	public BytecodeMethod getWriteIntMethod() {
		return writeIntMethod;
	}

	public BytecodeMethod getWriteStringMethod() {
		return writeStringMethod;
	}

	public BytecodeMethod getReadIntMethod() {
		return readIntMethod;
	}

	public BytecodeMethod getReadStringMethod() {
		return readStringMethod;
	}
	
	@Override
	public String toString() {
		var text = "";
		for (var method : methods) {
			text += method + "\r\n";
		}
		return text;
	}
}
