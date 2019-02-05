package hu.ddsi.java.database;


import java.util.Arrays;
import java.util.Iterator;

/**
 * Emékszem egyszer az SQL-es implemetációnál kiadta megy SELECT * FROM-ot,
 * A java meg is csinálta... volna ha nem szállt volna el OutOfMemoryException-nel.
 * Ezért itt Cursorral járjuk az adatbázist, kényelmi funkcióként tudjunk hát benne iterálni is.
 * */
/*public class GenericStoreDBCursor<T extends GenericStorable> implements Iterable<T>
{
	private final GenericStoreQueryResult res;
	private final GenericStoreDatabase gdb;
	private final Class<? extends GenericStorable>[] clss;
	private int pointer;
	
	public GenericStoreDBCursor(GenericStoreQueryResult res,GenericStoreDatabase gdb,Class<? extends GenericStorable>[] clss)
	{
		this.res = res;
		this.gdb = gdb;
		this.clss = clss;
	}
	
	public GenericStoreDBCursor()
	{
		this.res = new GenericStoreQueryResult<>(new long[0]);
		this.gdb = null;
		this.clss = null;
	}
	
	/**
	 * Ez az iterator next() metódusnál RuntrimeException dobhat!
	 * Ekkoraz elemeket darabonként kérjük ki.
	 * */
/*	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			@Override
			public boolean hasNext()
			{
				return pointer < res.getIds().length;
			}

			@Override
			public T next()
			{
				try
				{
					//Ez a publish-t is megoldja
					return (T) gdb.getSingleObjectByID(res.getIds()[pointer++],clss);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}

			@Override
			public void remove()
			{}
		};
	}
	
	
	
	public int size()
	{
		return res.getIds().length;
	}
	
	public long[] getIDs()
	{
		return Arrays.copyOf(res.getIds(), res.getIds().length);
	}
}*/