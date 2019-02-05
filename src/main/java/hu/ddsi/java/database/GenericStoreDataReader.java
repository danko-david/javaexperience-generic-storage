package hu.ddsi.java.database;

import java.util.Date;

import eu.javaexperience.reflect.Mirror;

/**
 * Ez az objektum immutable lett, minden osztályhoz csak egy lesz példányosítva
 * 
 * Az eljárás az alábbi:
 * 1) directGetObject(long id): gyorsítótárazás esetén ha az a tárban van, közvetlen vissza lehet adni.
 * 2) getClassNameByID(long id): az adott ID-hez milyen osztály tartozik? Ebből egy java példány készül majd átadódik:
 * 3) putRawObject(GenericStorable obj)-nek a mezők feltöltése belső implementációval is megvalósítható, ha ez a metóus
 * 		false-ot ad vissza akkor a 4-es metódus nem hajtódik végre.
 * 4) readField() és readArray(), végigiterálnak az összes mezőn az implementáló metódustól elkérve és a példányba beleírja.
 * 5) getProcessedObject()-val visszakéri a feldolgozott objektumot.
 * */
public abstract class GenericStoreDataReader<T>
{
	/*
	protected final String cls;
	protected final GenericStoreDatabase db;
	public GenericStoreDataReader(GenericStoreDatabase db, String cls)
	{
		this.db = db;
		this.cls = cls;
	}
	*/
	
	public static <T extends GenericStorable> T[] convBack(Long[] ids,Class<? extends T> desc,GenericStoreDatabase gdb) throws Exception
	{
		if(null == ids)
		{
			return null;
		}
		
		T[] ret = (T[]) java.lang.reflect.Array.newInstance(desc, ids.length);
		for(int i=0;i<ret.length;i++)
		{
			final int index = i;
			if(ids[i] == -1)
			{
				ret[i] = null;
			}
			else
			{
				gdb.putPlacementRequest(desc, ids[i], (e)->{ret[index] = (T) e;});
				//ret[i] = GenericStorage.getObjectByIDDescendantOf(ids[i], desc, gdb);
			}
		}
		return ret;
	}
	
	/**
	 * TODO még átadás előtt belek kell tenni a tárolási adatokat id és a többit, gyorsítótárazni...
	 * TODO ezelőtt nézd meg a gyorsítótárban!
	 * */
	public GenericStorable readObject(long id,T src,GenericStorable ret,GenericStoreDatabase db,FieldData[] fds) throws Exception
	{
			for(FieldData fd:fds)
				switch (fd.type)
				{
				case Boolean:
				case Byte:
				case Double:
				case Float:
				case Integer:
				case Short:
				case String:
				case Long:
					Object obj = readField(fd.f.getName(),fd.type,src);
					if(obj != null)
						fd.f.set(ret,obj );
					break;	
	
				case Date:
					fd.f.set(ret, (Date)readField(fd.f.getName(),fd.type,src));
					break;
	
				case Array:
					if(fd.getArrayData().primitivType == GenericStoreDataType.GenericDataId)
						fd.f.set(ret, convBack((Long[]) readArray(fd.f.getName(), fd.arr, src), (Class<GenericStorable>)Mirror.getFinalComponentClass(fd.f.getType()), db));
					else
						fd.f.set(ret, readArray(fd.f.getName(), fd.arr, src));
					break;
					
				case Enum:
					Object ob = readField(fd.f.getName(),fd.type,src);
					if(ob != null)
						fd.f.set(ret, fd.f.getType().getEnumConstants()[(Integer)ob ]);
					break;

				case Serialized:
					fd.f.set(ret, readSerializedField(fd.f.getName(),src));
					break;
					
				case GenericDataId:
					Object o = readField(fd.f.getName(),fd.type,src);
					if(o != null)
						try
						{
							db.putPlacementRequest(fd.f.getType(), ((Number)o).longValue(), (e)->
							{
								try
								{
									fd.f.set(ret, e);
								}
								catch (Exception e1){}
							});
							
							//fd.f.set(ret, db.getSingleObjectByID(((Number)o).longValue(), db.getDescendantClassesFor((Class<? extends GenericStorable>) fd.f.getType())));
						}
						catch(Exception e)
					{
							e.printStackTrace();
					}
					break;
					
				case DontStore:
					continue;
				}
		
		return ret;
	}
	
	protected abstract Object readField(String name,GenericStoreDataType type, T src)throws Exception;
	protected abstract Object readSerializedField(String name, T src) throws Exception;
	protected abstract Object[] readArray(String name,GenericStoreDataArray arr, T src)throws Exception;
	
	protected abstract String getClassBy(T cur) throws Exception;
	
	protected abstract long getIdBy(T cur) throws Exception;
	
	/**
	 * Beállítja a következő eredményt, és true-val tér vissza.
	 * false ha nincs több eredmény
	 * */
	protected abstract boolean nextResult(T cur) throws Exception;
	
	protected abstract boolean setReadyToRead(T cur) throws Exception;
}