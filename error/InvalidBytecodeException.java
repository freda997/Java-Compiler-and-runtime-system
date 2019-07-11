package error;

public class InvalidBytecodeException extends VMException {
	private static final long serialVersionUID = 1L;

	public InvalidBytecodeException(String message) {
		super("Invalid bytecode: " + message);
	}
}
