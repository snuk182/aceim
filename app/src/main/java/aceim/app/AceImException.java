package aceim.app;

public class AceImException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AceImExceptionReason reason;
	
	public AceImException(AceImExceptionReason reason) {
		this.reason = reason;
	}

	public AceImException(String message, AceImExceptionReason reason) {
		super(message);
		this.reason = reason;
	}
	
	public AceImException(Throwable t, AceImExceptionReason reason) {
		super(t);
		this.reason = reason;
	}
	
	public static enum AceImExceptionReason {
		EXCEPTION,
		STORAGE_ERROR,
		NO_PROTOCOL_FOUND,
		RESOURCE_NOT_INITIALIZED;
	}
}
