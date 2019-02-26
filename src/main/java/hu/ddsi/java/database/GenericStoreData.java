package hu.ddsi.java.database;

import hu.ddsi.java.database.GenericStoreData.GenericStorageObjectState;

public class GenericStoreData
{
	public GenericStoreData(){}
	GenericStoreData(long id)
	{
		this.id = id;
		this.state = GenericStorageObjectState.PERSISTED;
	}
	
	public boolean isModified()
	{
		return state.isModified;
	}
	
	public GenericStorageObjectState getState()
	{
		return state;
	}
	
	public long getID()
	{
		return id;
	}
	
	public boolean isObjectNewAndUnsaved()
	{
		return GenericStorage.NewObjectNotSaved == id;
	}
	
	public boolean isObjectDeletedUntacked()
	{
		return GenericStorage.ObjectUntrackedUnstored == id;
	}
	
	public GenericStoreDatabase getOwnerDatabase()
	{
		return owner;
	}
	
	public static enum GenericStorageObjectState
	{
		NEW(true),
		MODIFIED(true),
		UNDER_SAVE(false),
		PERSISTED(false),
		DELETED(false)
		
		;
		
		final boolean isModified;
		
		private GenericStorageObjectState(boolean mod)
		{
			this.isModified = mod;
		}
		
	}
	
	long id = -1;
	GenericStorageObjectState state = GenericStorageObjectState.NEW;
	GenericStoreDatabase owner;
	
	public void setOwnerDatabase(GenericStoreDatabase gdb)
	{
		owner = gdb;
	}
	public void setId(Long long1)
	{
		id = long1;
	}
}