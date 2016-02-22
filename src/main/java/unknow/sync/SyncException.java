package unknow.sync;

public class SyncException extends Exception
	{
	private static final long serialVersionUID=1L;

	public SyncException()
		{
		super();
		}

	public SyncException(String msg)
		{
		super(msg);
		}

	public SyncException(Throwable cause)
		{
		super(cause);
		}

	public SyncException(String msg, Throwable cause)
		{
		super(msg, cause);
		}
	}
