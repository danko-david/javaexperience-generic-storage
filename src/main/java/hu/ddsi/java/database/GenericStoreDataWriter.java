package hu.ddsi.java.database;

import java.util.List;

public abstract class GenericStoreDataWriter<D extends GenericStoreDatabase,W>
{
	public abstract void writeField(String name, FieldData fd,Object value,W w) throws Exception;
	
	public abstract void writeArrayField(String name,GenericStoreDataArray arr,Object value,W w) throws Exception;

	public abstract W newWriter(D db, Class<? extends GenericStorable> store,boolean update);
	
	public static Object conventToLongArray(GenericStorable[] arr,GenericStoreDatabase gdb) throws GenericStoreException
	{
		Long[] ret = new Long[arr.length];
		for(int i=0;i<ret.length;i++)
			if(arr[i] == null)
				ret[i] = -1L;
			else
			{
				long id = GenericStorage.getID(arr[i]);
				if(id == -1)
				{
					GenericStorage.storeObject(arr[i], gdb);
				}
				ret[i] = GenericStorage.getID(arr[i]);
			}
		
		return ret;
	}
	
	public void writeObject(long id,GenericStorable stora, D gdb, FieldData[] fields,boolean update) throws Exception
	{
		W write = newWriter(gdb,stora.getClass(),update);
		
		for(FieldData fd:fields)
		{
			Object val = fd.getField().get(stora);
			//if(val == null)
			//	continue;
			
			if(fd.isArray())
			{
				if(fd.getArrayData().primitivType == GenericStoreDataType.GenericDataId)
					writeArrayField(fd.getField().getName(), fd.getArrayData(), conventToLongArray((GenericStorable[])val,gdb),write);
				else
					writeArrayField(fd.getField().getName(), fd.getArrayData(), val,write);
			}
			else
				writeField(fd.getField().getName(), fd, val, write);
		}
		
		writeID(id,write);
		flushObject(write);
	}
	
	/**
	 * Mentési hurkok megakadályozása miatt előre tudnunk kell az objektum leendő azonosítóját.
	 * Ha kereszbe hivatkozik két objektum mezője a másik objektumra akkor egymást próbálnák
	 * végtelen ciklusba menteni.
	 * Bár ebben az esetben előfordulhat az hogy egy objektum lementése sikertelen, ekkor az
	 * egyik objektum azonosítója érvénytelen objektumra fog mutatni.
	 * */
	public abstract void flushObject(W w) throws Exception;
	
	public abstract void writeID(long id,W w) throws Exception;

	public abstract void writeObjects(List<? extends GenericStorable> objects, GenericStoreDatabase gdb, FieldData[] fds) throws Exception;
}