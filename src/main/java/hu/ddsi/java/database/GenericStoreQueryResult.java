package hu.ddsi.java.database;

import java.io.Closeable;
import java.io.IOException;

public class GenericStoreQueryResult<R extends Closeable> implements Closeable
{
	private final long[] ids;
	private final ResultUnit<R>[] results;
	
	public static class ResultUnit<R extends Closeable>
	{
		private final String returnClass;
		private final R cursor;
		
		public ResultUnit(String cls,R cur)
		{
			returnClass = cls;
			cursor = cur;
		}
		
		public String getReturnClass()
		{
			return returnClass;
		}
		
		public R getCursor()
		{
			return cursor;
		}

		public void close() throws IOException
		{
			cursor.close();
		}
	}
	
	public GenericStoreQueryResult(long[] ids,ResultUnit<R>... results)
	{
		this.ids = ids;
		this.results = results;
	}

	public ResultUnit<R>[] getResults()
	{
		return results;
	}

	@Override
	public void close() throws IOException
	{
		for(ResultUnit<R> r:results)
		{
			r.close();
		}
	}
}