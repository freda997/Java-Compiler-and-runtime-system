package jit;

import com.sun.jna.Library;
import com.sun.jna.Native;

import runtime.descriptors.TypeDescriptor;

public class JITNative {
	public interface CLibrary extends Library {
		CLibrary INSTANCE = (CLibrary) Native.load("NativeCall", CLibrary.class);
		
		int call(byte[] code, int codeLength, int[] arguments, int nofArguments);
	}

	public static Object call(byte[] code, Object[] arguments, TypeDescriptor returnType) {
		var input = new int[arguments.length];
		for (int index = 0; index < arguments.length; index++) {
			var value = arguments[index];
			if (value instanceof Integer) {
				input[index] = (int)value;
			} else if (value instanceof Boolean) {
				input[index] = (boolean)value ? 1 : 0;
			} else {
				throw new AssertionError("Unsupported argument type");
			}
		}
		int result = CLibrary.INSTANCE.call(code, code.length, input, input.length);
		if (returnType == null) {
			return null;
		} else if (returnType == TypeDescriptor.BOOLEAN_TYPE) {
			return result != 0; 
		} else if (returnType == TypeDescriptor.INT_TYPE) {
			return result;
		} else {
			throw new AssertionError("Unsupported return type");
		}
	}
}
