package hu.ddsi.java.database;

import hu.ddsi.java.database.GenericStoreData.GenericStorageObjectState;
import hu.ddsi.java.database.JavaSQLImp.SqlStorage;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.query.F;
import eu.javaexperience.query.LogicalGroup;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.semantic.references.MayNull;

/**
 * S: Nem lehetséges minden adatbázist egyformán megszólítani.
 * Dehogynem! Jól meghározott feltételekkel minden adatbázis megszólítható, az implementáció lényege pontosan ez (bár cinikusan mondhatnánk azt is hogy mindenhol ugyanolyan rosszul.)
 * Azaz ha például a SQL-ben nem használunk relációt, MongoDB-ben pedig a tipus és struktúrafüggetlen tárolást, akkor jutunk ehhez a megoldáshoz.
 * 
 * Eszerint célszerű objektumokban gondolkodnunk. Képzeljük azt hogy van egy nagy Map<Long,Object>-ünk.
 * 
 * Belső működés:
 * 		- Minden olyan osztálynak, amit tárolni szeretnénk implementálnia kell a {@link GenericStorable} interfacet-t.
 * 		- Az egyes mezőket - attól függően hogy hogyan szeretnénk tárolni - annotációkkal kell ellátni.
 * 		- Az objektumok és tömbök általános tárolási módja a sorosítás, így tartalmában nem kereshetünk.
 * 		- A GenericStorable egy keretet biztosít, a végrehajtást a {@link GenericStoreDatabase}, {@link GenericStoreDataReader} és {@link GenericStoreDataWriter} implementációi végzik.
 * 
 * Mezők tárolásának a lehetősége:
 * 			- A statikus mezők nem kerülnek tárolásra.
 * 			- Azok a mezők amelyeket nem jelöltünk semmilyen annotációval nem vesznek rész a tárolásban.
 * 			- Szülő osztály mezőit is lehet tárolni:
 * 
 * A tárolandó objektum implementálja a {@link GenericStorable} interface-t akkor az általa szolgáltatott {@link GenericStorable#getSelfDefinedMapping()} Map alapján történik az egyes mezők tárolása,
 * ha ez null akkor a természetes úton történik a mezők begyüjtése. (Mi van ha a felmenőkben két ugyanolyan nevű privát mező is volt (más-más felső osztályba)? Az Object osztályhoz közelebbi lesz figyelembe véve...)
 * 
 * Ezt a Map-et célszerű statikusan egyszer létrehozni és mindig ugyanazt adni vissza.
 * A benne lévő {@link GenericStoreData} azért szükséges hogy ne kelljen absztakt osztályt létrehozni, ezzel megfosztva a tervezendő osztályt az öröklés lehetőségétől. Azt a mezőt ami a tarolási adatokat tartalmazza jelöljük {@link GenericStoreIgnore}-ral
 * 
 * Általános adatbázisok:
 * Szükségünk lesz egy adatbázisra, ami lehet MySQL, PostgreSQL, SQLite, MongoDB vagy //TODO solr.
 * Ezeket a megfelelő implementációját már megírtam:
 * 		- Java JDBC motorra épülő SQL adatbázis kezelő: minden olyan Driver ami {@link java.sql.Connection}-nel tér vissza, tesztelve: MySQL, PostgreSQL, SQLite
 * 			Osztály neve: {@link SqlStorage}
 * 		- MongoDB, szükésg lesz a com.mongodb-ben lévő osztályokra, a {@link MongoDBStorageDatabase} egy DB objektumot vár amit a {@link Mongo#getDB(String)} ad vissza.
 * 		- solr //TODO
 * 		- ilyen implementációkat mi is készíthetünk {@link GenericDatabaseHowToImplement}.
 * 
 * Egy objektum útja az új adatbázisba:
 * 		//TODO
 *	
 * 		Ha létrehoztunk egy {@link GenericStorable} példányt és az tárolni szeretnénk adjuk
 * 
 * 
 * Az objekum azonosítója az adatbázisban "do" névvesz szerepel, mivel ez a java-ban kulcsszó nem fedünk el használható mezőnevet.  
 * 
 * 
 * //jó lenne ha lehetne Mapot is beállítani hogy az itteni java mezők milyen adatbázis mezőknek felelnek meg! 
 * // bár az inkább adatbázis specifikus beállítás 
 * */
public class GenericStorage
{
	public static final long ObjectUntrackedUnstored = -2;
	public static final long NewObjectNotSaved = -1;
	
	public static FieldData getFieldByName(FieldData[] data, String name)
	{
		for(FieldData d:data)
		{
			if(d.f.getName().equals(name))
			{
				return d;
			}
		}
		
		return null;
	}
	/*
	public static FieldData[] getOrCollectClassData(String strCls) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		Class<? extends GenericStorable> cls = (Class<? extends GenericStorable>) stringToClass.get(strCls);
		
		if(cls != null)
			return classesFieldDatas.get(cls);
		FieldData[] data = getOrCollectClassData((Class<? extends GenericStorable>)Class.forName(strCls));
		cls = (Class<? extends GenericStorable>) stringToClass.get(strCls);
		return data;
	}
	
	static Class<?> getOrCollectClass(String clsStr) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		Class<?> cls = stringToClass.get(clsStr); 
		if(cls != null)
			return cls;
		
		getOrCollectClassData(clsStr);
		
		return stringToClass.get(clsStr);
	}
	*/
	public static FieldData[] getOrCollectClassDataRt(Class<? extends GenericStorable> cls)
	{
		try
		{
			return getOrCollectClassData(cls);
		}
		catch(Exception e)
		{
			Mirror.propagateAnyway(e);
			return null;
		}
	}
	
	public static FieldData[] getOrCollectClassData(Class<? extends GenericStorable> cls) throws InstantiationException, IllegalAccessException
	{
		return GenericStorageMappingData.getOrCollectClassData(cls);
	}
	
	public static <T extends GenericStorable> T newInstance(Class<T> cls) throws InstantiationException, IllegalAccessException
	{
		//return UnsafeMirror.allocObject(cls);
		return cls.newInstance();
	}
	
	/*static FieldData[] getStorageForClass(GenericStorable obj,GenericStoreDatabase db) throws InstantiationException, IllegalAccessException, GenericStoreException
	{
		try
		{
			FieldData[] fds = classesFieldDatas.get(obj.getClass());
			
			if(fds == null)
				fds = GenericStorageMappingData.getOrCollectClassData(obj);
			
			db.createStorageForClassIfNeeded(obj.getClass(), fds);
			
			return fds;
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}
	}*/

	/*public static void createStorageForClassIfNeeded(Class<? extends GenericStorable> cls, GenericStoreDatabase db) throws Exception
	{
		FieldData[] fds = classesFieldDatas.get(cls);
		
		if(fds == null)
			fds = getOrCollectClassData(cls);
		
		db.createStorageForClassIfNeeded(cls, fds);
	}*/
	
	@Deprecated
	public static GenericStorable getObjectByID(long id,GenericStoreDatabase gdb) throws GenericStoreException
	{
		return getObjectByIDDescendantOf(id, GenericStorable.class, gdb);
	}
	
	public static <T extends GenericStorable> T getObjectByIDDescendantOf(long id,Class<? extends T> cls,GenericStoreDatabase gdb) throws GenericStoreException
	{
		try
		{
			return (T) gdb.getSingleObjectByID(id, gdb.getDescendantClassesFor(cls));
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}
	}
	
	public static <D extends GenericStoreDatabase,W> void storeObject(GenericStorable gs,D gdb) throws GenericStoreException
	{
		GenericStoreData data = gs.getGenericStoreData();
		if(data != null)
		{
			if(gs.getGenericStoreData().id != -1 && (GenericStorageObjectState.UNDER_SAVE == data.state || !gs.getGenericStoreData().isModified()))
			{
				return;
			}
		}
		try
		{
			gdb.store(gs);
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e.getMessage()+" "+gs.toString(),e);
		}
	}
	
	public static GenericStoreData getOrCreateGenericStoreData(GenericStorable gs)
	{
		GenericStoreData data = gs.getGenericStoreData();
		if(data == null)
		{
			gs.setGenericStoreData(data = new GenericStoreData());
		}
		return data;
	}
	
	public static void storeAll(Collection<? extends GenericStorable> coll,GenericStoreDatabase gdb) throws GenericStoreException
	{
		try
		{
			gdb.storeAll(coll);
		}
		catch(Exception e)
		{
			throw new GenericStoreException(e);
		}
	}
	
	public static void storeAll(GenericStoreDatabase gdb, GenericStorable... gss) throws GenericStoreException
	{
		try
		{
			gdb.storeAll(CollectionTools.inlineAdd(new ArrayList(), gss));
		}
		catch(Exception e)
		{
			throw new GenericStoreException(e);
		}
	}
	
	public static <T extends GenericStorable> void getAllObjectsByQuery(Class<? extends T> cls, LogicalGroup lg,Collection<T> coll,GenericStoreDatabase gdb, GsdbExtraCaluse... extra) throws GenericStoreException
	{
		try
		{
			gdb.getAllObjectsByQuery(cls, lg, coll, extra);
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}		
	}
	
	/*public static <T extends GenericStorable> GenericStoreDBCursor<T> getObjectsByQuery(Class<T> cls,LogicalGroup lg,GenericStoreDatabase gdb) throws GenericStoreException
	{
		try
		{
			if(!gdb.isStored(cls))
				return new GenericStoreDBCursor<>();
			return new GenericStoreDBCursor<T>(gdb.getIDListByQuery(cls, lg,true), gdb,new Class[]{cls});
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}		
	}*/
	
	/*public static <T extends GenericStorable> GenericStoreDBCursor<T> getObjectsByNativeStringQuery(Class<T> cls,String query,GenericStoreDatabase gdb) throws GenericStoreException
	{
		try
		{
			if(!gdb.isStored(cls))
				return new GenericStoreDBCursor<>();
			return new GenericStoreDBCursor<T>(gdb.getIDListByNativeStringQuery(cls, query), gdb,new Class[]{cls});
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}		
	}*/
	
	public static void removeObject(GenericStorable gd, GenericStoreDatabase gdb) throws GenericStoreException
	{
		GenericStoreData data = gd.getGenericStoreData();
		if(data == null)
			return;
		
		if(data.id < 0)
			return;
		
		try
		{
			gdb.deleteObjectByIDSByClass(new long[]{data.id}, new Class[]{gd.getClass()});
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}
		untraceObject(gd);
	}
	
	public static <A extends Collection<B>,B extends GenericStorable> void bulkRemoveObject(A gs, GenericStoreDatabase gdb) throws GenericStoreException
	{
		HashMap<Class<? extends GenericStorable>, List<GenericStorable>> map = new HashMap<>();
		
		for(GenericStorable g:gs)
		{
			if(g == null)
				continue;
			
			GenericStoreData data = g.getGenericStoreData();
			if(data == null)
				continue;
			
			if(data.id < 0)
				continue;
			
			List<GenericStorable> c = map.get(g.getClass());
			if(c == null)
				map.put(g.getClass(), c = new ArrayList<>());
			
			c.add(g);
		}
		
		
		try
		{
			for(Entry<Class<? extends GenericStorable>, List<GenericStorable>> kv:map.entrySet())
			{
				List<GenericStorable> l = kv.getValue();
				long[] idz = new long[l.size()];
				for(int i=0;i<idz.length;++i)
					idz[i] = getID(l.get(i));
				
				gdb.deleteObjectByIDSByClass(idz, new Class[]{kv.getKey()});
			}
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}
		
		for(GenericStorable g:gs)
			if(g != null)
				untraceObject(g);
	}
	
	/**
	 * visszaadja az adatbázisban való tárolásnál használt egyedi azonosítót,
	 * ha még nem lett eltárolva akkor -1 -et
	 * */
	public static long getID(GenericStorable gs)
	{
		return gs.getGenericStoreData() == null?-1:gs.getGenericStoreData().id;
	}
	
	public static boolean isStored(GenericStorable gs)
	{
		GenericStoreData data = gs.getGenericStoreData();
		return data != null && data.owner != null;
	}
	
	public static GenericStoreDatabase getOwnerDatabase(GenericStorable gs)
	{
		return gs.getGenericStoreData() == null?null:gs.getGenericStoreData().owner;
	}
	
	public static void objectModified(GenericStorable st)
	{
		if(st.getGenericStoreData() != null)
			st.getGenericStoreData().state = GenericStorageObjectState.MODIFIED;
	}
	
	public static void untraceObject(GenericStorable gs)
	{
		gs.getGenericStoreData().id = ObjectUntrackedUnstored;
		gs.getGenericStoreData().owner = null;
		gs.getGenericStoreData().state = GenericStorageObjectState.DELETED;
	}
	
	

	public static void fillFieldData
	(
		Class<? extends GenericStorable> type,
		Collection<FieldData> dst
	)
		throws InstantiationException, IllegalAccessException
	{
		FieldData[] fds = getOrCollectClassData(type);
		if(null != fds)
		{
			for(FieldData f:fds)
			{
				dst.add(f);
			}
		}
	}
	
	public static <T extends GenericStorable> void post(GenericStoreDatabase gdb, T j) throws GenericStoreException
	{
		GenericStoreDatabase odb = getOwnerDatabase(j);
		if(null != odb)
		{
			if(!odb.equals(gdb))
			{
				throw new RuntimeException("Object from different database");
			}
		}
		
		if(isStored(j))
		{
			objectModified(j);
		}
		
		storeObject(j, gdb);
	}
	
	public static <C extends Collection<GenericStorable>> C getReferences(GenericStorable subject, C refs) throws Exception
	{
		Class scls = subject.getClass();
		GenericStoreDatabase gdb = GenericStorage.getOwnerDatabase(subject);
		
		List<Class<? extends GenericStorable>> classes = gdb.getDescendantClassesFor(GenericStorable.class);
		for(Class c:classes)
		{
			if(c == GenericStorable.class)
			{
				continue;
			}
			FieldData[] fs = GenericStoreDatabase.getOrCreateFieldData(c);
			for(FieldData f:fs)
			{
				if(scls.isAssignableFrom(f.f.getType()))
				{
					GenericStorage.getAllObjectsByQuery(c, F.eq.is(f.f.getName(), subject), refs, gdb);
				}
			}
		}
		
		return refs;
	}

	public static <T extends GenericStorable> List<T> getObjectsByQuery(Class<T> class1, LogicalGroup lg, GenericStoreDatabase gdb) throws GenericStoreException
	{
		ArrayList<T> ret = new ArrayList<T>();
		getAllObjectsByQuery(class1, lg, ret, gdb);
		return ret;
	}

	public static <T extends GenericStorable> long[] getIds(Collection<T> objects)
	{
		long[] ret = new long[objects.size()];
		int i = 0;
		for(T o:objects)
		{
			ret[i++] = getID(o);
		}
		return ret;
	}

	public static <T extends GenericStorable> void postAll(GenericStoreDatabase gdb, Collection<T> els) throws GenericStoreException
	{
		for(T e:els)
		{
			if(isStored(e))
			{
				objectModified(e);
			}
		}
		
		storeAll(els, gdb);
	}

	public static <T extends GenericStorable> void getObjectsByQuery(GenericStoreDatabase gdb, Collection<T> ents, Class<T> cls, LogicalGroup lg, GsdbExtraCaluse... extra) throws GenericStoreException
	{
		getAllObjectsByQuery(cls, lg, ents, gdb, extra);
	}
	
	public static void update(GenericStorable elem) throws GenericStoreException
	{
		GenericStoreDatabase gdb = getOwnerDatabase(elem);
		if(null == gdb)
		{
			throw new RuntimeException("Object not yet stored, can't update");
		}
		
		post(gdb, elem);
	}
	
	/*
	TODO move other package
	public static <T extends GenericStorable> DataArray exportDatabaseCollecion(DataCommon comm,GenericStoreDatabase gdb,Class<T> cls) throws GenericStoreException
	{
		DataArray ret = comm.newArrayInstance();
		try
		{
			List<T> lst = new ArrayList<>();
			getAllObjectsByQuery(cls, F.gt.is("do", -1), lst, gdb);
			FieldData[] fds = getOrCollectClassData(cls);
			
			for(T s:lst)
			{
				DataObject crnt = comm.newObjectInstance();
				for(FieldData fd:fds)
				{
					Object val = fd.getField().get(s);
					if(val == null)
						continue;
					
					if(fd.isArray())
					{
						DataArray arr = comm.newArrayInstance();
						for(Object o:((Object[])val))
							DataReprezTools.put(arr,o);

						crnt.putArray(fd.getField().getName(), arr);
					}
					else
						DataReprezTools.put(crnt,fd.getField().getName(),val);
				}
				ret.putObject(crnt);
			}	
		}
		catch(Exception e)
		{
			throw new GenericStoreException(e);
		}
		
		return ret;
	}

	public static <T extends GenericStorable> void copyFields(DataObject src, T dst) throws IllegalArgumentException, IllegalAccessException, InstantiationException
	{
		FieldData[] fs = GenericStorage.getOrCollectClassData(dst.getClass());
		for(FieldData f:fs)
		{
			CastTo ct = CastTo.getCasterForTargetClass(f.f.getType());
			Object set = src.get(f.f.getName());
			if(null != ct)
			{
				set = ct.cast(set);
			}
			f.f.set(dst, set);
		}
	}
	*/
}