package hu.ddsi.java.database;

public class GenericStoreException extends Exception
{
	private static final long serialVersionUID = 1L;

	public GenericStoreException(String reason)
	{
		super(reason);
	}
	
	public GenericStoreException(String reason,Throwable t)
	{
		super(reason,t);
	}

	public GenericStoreException(Throwable t)
	{
		super(t);
	}
}