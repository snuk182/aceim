package aceim.api.service;

/**
 * Exception used in several core calls. See {@link ICoreProtocolCallback#connectionStateChanged(byte, aceim.api.dataentity.ConnectionState, int)} for sample.
 */
public class ProtocolException extends Exception {
	
	public final Cause cause;
	
	public ProtocolException(String message) {
		this(Cause.DEFAULT, message);
	}
	
	public ProtocolException(Cause cause, String message) {
		super(message);
		this.cause = cause;
	}

	public ProtocolException(Cause cause) {
		this(cause, null);
	}
	
	private static final long serialVersionUID = 5676297681749673246L;
	
	public enum Cause {
		/**
		 * No error.
		 */
		NONE,
		
		/**
		 * General error, see exception message.
		 */
		DEFAULT,
		
		/**
		 * The multi-user chat you are trying to create already exists (has same protocol UID).
		 */
		GROUPCHAT_ALREADY_EXISTS,
		
		/**
		 * The multi-user chat you are trying to join does not exist (no such protocol UID).
		 */
		NO_GROUPCHAT_AVAILABLE,
		
		/**
		 * Some data required for authentication is missing.
		 */
		BROKEN_AUTH_DATA,
		
		/**
		 * Connection error.
		 */
		CONNECTION_ERROR,
		
		/**
		 * Incorrect authentication data.
		 */
		CANNOT_AUTHORIZE,
		
		/**
		 * Connection (or ping rate) limit exceeded.
		 */
		CONNECTION_LIMIT_EXCEEDED;
	} 
}
