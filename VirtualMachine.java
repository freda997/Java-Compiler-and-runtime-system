import java.io.IOException;

import bytecode.BytecodeAssembly;
import error.VMException;
import runtime.Interpreter;
import runtime.Loader;

public class VirtualMachine {
	public static void main(String[] arguments) {
		if (arguments.length != 1) {
			System.out.println("Usage: java VirtualMachine <file>");
			return;
		}
		var inputFile = arguments[0];
		try {
			var assembly = BytecodeAssembly.load(inputFile);
			var loader = new Loader(assembly);
			var interpreter = new Interpreter(loader, true);
			interpreter.run();
		} catch (VMException exception) {
			System.out.println("VM ERROR: " + exception.getMessage());
		} catch (IOException exception) {
			System.out.println("INPUT ERROR: " + exception.getMessage());
		}
	}
}
