package unknow.sync;

/**
 * @author unknow
 */
public class SyncException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * create new SyncException
	 */
	public SyncException() {
		super();
	}

	/**
	 * create new SyncException
	 * 
	 * @param msg the message
	 */
	public SyncException(String msg) {
		super(msg);
	}

	/**
	 * create new SyncException
	 * 
	 * @param cause the cause
	 */
	public SyncException(Throwable cause) {
		super(cause);
	}

	/**
	 * create new SyncException
	 * 
	 * @param msg   the message
	 * @param cause the cause
	 */
	public SyncException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
