package error;

public class VMException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public VMException(String message) {
		super(message);
	}
}
