package hu.ddsi.java.database;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.reflect.Mirror;
import hu.ddsi.java.database.FieldData;
import hu.ddsi.java.database.GenericStorable;
import hu.ddsi.java.database.GenericStoreDataReader;
import hu.ddsi.java.database.GenericStoreDataWriter;
import hu.ddsi.java.database.GenericStoreDatabase;
import hu.ddsi.java.database.GenericStoreQueryBuilder.LogicalGroup;
import hu.ddsi.java.database.GenericStoreQueryResult;

public class PermissionWrappedGdb extends GenericStoreDatabase
{
	protected GenericStoreDatabase gdb;
	protected SimpleGet<Set<String>> permissions;
	public PermissionWrappedGdb(GenericStoreDatabase gdb, SimpleGet<Set<String>> perms)
	{
		this.gdb = gdb;
		this.permissions = perms;
		try
		{
			mustCallAfterConnectionEstablishedBeforeUse();
		}
		catch(Exception e)
		{
			Mirror.propagateAnyway(e);
		}
	}
	
	public GenericStoreDatabase getBackend()
	{
		return gdb;
	}

	protected void resolvePlacements() throws IOException, Exception
	{
		gdb.resolvePlacements();
	}
	
	protected GenericStorable getFromCacheOrPublishRead(long id, Object res, Class<GenericStorable> re, String cls, FieldData[] fds) throws Exception
	{
		return gdb.getFromCacheOrPublishRead(id, res, re, cls, fds);
	}
	
	protected void setupReadedObject(GenericStorable gs,long id)
	{
		gdb.setupReadedObject(gs, id);
	}
	
	protected GenericStorable getSingleObjectByID(long id, List<Class<? extends GenericStorable>> clss) throws Exception
	{
		return gdb.getSingleObjectByID(id, clss);
	}
	
	public List<Class<? extends GenericStorable>> getDescendantClassesFor(Class<? extends GenericStorable> cls) throws Exception
	{
		return gdb.getDescendantClassesFor(cls);
	}
	
	public boolean isStored(Class<? extends GenericStorable> cls) throws Exception
	{
		return gdb.isStored(cls);
	}
	
	protected boolean isStored(Class<? extends GenericStorable> cls,String[] lst)
	{
		return gdb.isStored(cls, lst);
	}
	
	@Override
	public GenericStoreDataWriter getWriter(String cls) throws Exception
	{
		return gdb.getWriter(cls);
	}
	@Override
	public String getDatabaseName()
	{
		return gdb.getDatabaseName();
	}
	
	protected void assertPermission(String perm)
	{
		if(!permissions.get().contains(perm))
		{
			throw new RuntimeException("You have no `"+perm+"` permission for this database");
		}
	}
	
	@Override
	public GenericStoreQueryResult getIDListByQuery(Class<? extends GenericStorable> cls, LogicalGroup lg, boolean all_field) throws Exception
	{
		assertPermission("read");
		return gdb.getIDListByQuery(cls, lg, all_field);
	}
	@Override
	public void createStorageForClass(Class<? extends GenericStorable> cls, FieldData[] data) throws Exception
	{
		assertPermission("write");
		gdb.createStorageForClass(cls, data);
	}
	
	@Override
	protected void dropClassStorageImpl(Class<? extends GenericStorable> cls) throws Exception
	{
		assertPermission("delete");
		gdb.dropClassStorage(cls);
	}
	
	@Override
	public void deleteObjectByIDSByClass(long[] id, Class<? extends GenericStorable>[] cls) throws Exception
	{
		assertPermission("delete");
		gdb.deleteObjectByIDSByClass(id, cls);
	}
	
	@Override
	public String[] listStoredClasses() throws Exception
	{
		assertPermission("read");
		return gdb.listStoredClasses();
	}
	
	@Override
	public GenericStoreDataReader getReader(String clas) throws Exception
	{
		return gdb.getReader(clas);
	}
	
	@Override
	public void storeAll(Map<Class<? extends GenericStorable>, List<? extends GenericStorable>> map) throws Exception
	{
		assertPermission("write");
		gdb.storeAll(map);
	}
	
	@Override
	protected List<Long> reserveNextIDRangeAtomic(int size) throws Exception
	{
		assertPermission("create");
		return gdb.reserveNextIDRangeAtomic(size);
	}
	
	@Override
	protected long getCurrentId() throws Exception
	{
		return gdb.getCurrentId();
	}
	
	@Override
	public void close()
	{
		gdb.close();
	}
}
