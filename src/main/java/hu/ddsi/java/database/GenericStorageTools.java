package hu.ddsi.java.database;

import java.util.ArrayList;
import java.util.List;

import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.query.F;
import eu.javaexperience.query.LogicalGroup;

public class GenericStorageTools
{
	public static <T extends GenericStorable> T getOrCreate(GenericStoreDatabase gdb,T obj,String field,Object fieldVal) throws GenericStoreException
	{
		/*GenericStoreDBCursor<T> cur = (GenericStoreDBCursor<T>) GenericStorage.getObjectsByQuery(obj.getClass(), F.eq.is(field, fieldVal), gdb);
		Iterator<T> it = cur.iterator();
		*/
		T elem = (T) tryGetOrNull(obj.getClass(), field, fieldVal, gdb);
		
		if(null != elem)
		{
			return elem;
		}
		else
		{
			GenericStorage.storeObject(obj, gdb);
		}
		
		return obj;
	}
	
	public static <T extends GenericStorable> T tryGetOrNull(Class<T> cls,String field,Object value,GenericStoreDatabase db) throws GenericStoreException
	{
		return getSingle(cls, F.eq.is(field, value), db);
	}

	public static <T extends GenericStorable> T getSingle(Class<T> class1, LogicalGroup lg, GenericStoreDatabase gdb) throws GenericStoreException
	{
		ArrayList<T> arr = new ArrayList<>();
		
		GenericStorage.getAllObjectsByQuery(class1, lg, arr, gdb);
		
		if(arr.size() > 0)
		{
			return arr.get(0);
		}
		else
		{
			return null;
		}
	}

	public static <G extends GenericStorable> void updateAllWhere(Class<G> type, LogicalGroup select, SimplePublish1<G> updater, GenericStoreDatabase gdb) throws GenericStoreException
	{
		ArrayList<G> fs = new ArrayList<>();
		GenericStorage.getAllObjectsByQuery(type, select, fs, gdb);
		for(G g:fs)
		{
			updater.publish(g);
		}
		GenericStorage.storeAll(fs, gdb);
	}
	
	public static <C extends GenericStorable> boolean checkFieldIsUniqueExcludeInstance(GenericStoreDatabase gdb, Class<C> cls, String field, Object value, C except) throws GenericStoreException
	{
		ArrayList<C> ret = new ArrayList<C>();
		GenericStorage.getAllObjectsByQuery(cls, F.eq.is(field, value), ret, gdb);
		
		if(0 == ret.size())
		{
			return true;
		}
		
		if(ret.size() > 1)
		{
			return false;
		}
		
		return ret.get(0) != value;
	}
	
	public static <G extends GenericStorable> List<G> getAllObjectOf(GenericStoreDatabase gdb, Class<G> cls) throws GenericStoreException
	{
		ArrayList<G> ret = new ArrayList<>();
		GenericStorage.getAllObjectsByQuery(cls, F.eq.not("do", -1), ret, gdb);
		return ret;
	}

	public static <G extends GenericStorable> List<G> queryAll(GenericStoreDatabase gdb, Class<G> cls, LogicalGroup lg) throws GenericStoreException
	{
		ArrayList<G> ret = new ArrayList<>();
		GenericStorage.getAllObjectsByQuery(cls, lg, ret, gdb);
		return ret;
	}
}