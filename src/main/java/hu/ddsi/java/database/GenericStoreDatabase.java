package hu.ddsi.java.database;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.javaexperience.collection.list.NullList;
import eu.javaexperience.collection.map.BulkTransitMap;
import eu.javaexperience.collection.map.MapTools;
import eu.javaexperience.collection.map.MultiMap;
import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.collection.set.OneShotList;
import eu.javaexperience.interfaces.ExternalDataAttached;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.SimpleGetFactory;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetByTools;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.math.MathTools;
import hu.ddsi.java.database.GenericStoreData.GenericStorageObjectState;
import hu.ddsi.java.database.GenericStoreQueryBuilder.LogicalGroup;
import hu.ddsi.java.database.GenericStoreQueryResult.ResultUnit;

/**
 * TODO:
 * 		- GSDB paramétereinek elrejtése
 * 		- enumok és date : 0 az null és nem ordinal! vagy 1970.01.01 00:00:000
 * 		- ha egy mező null lett az nem az jelenti hogy nem változott!
 * 		- enumok áteresztése (interface Hely extends GenericStorable, enum OrszagHely implements Hely)
 * 			enumok offsetelése - id (do) azonosító, enumok Integer.Maxint-el eltolva + nyilvántartó tábla
 * 
 * 		- modularizálhatóság: referencia állatás [Strong,Soft,Weak]
 * 		- átadható gyorsítótár
 * 
 * 		- ha egy új mező jelent meg
 * 		- létrehozás és módosítás dátuma.
 * 
 * Egyszer egy szép napon (heten/hónapon):
 * 
 * 		- Thread Safe:
 * 			- 2 fázisú felolvasás vagy felolvasászár, felolvasás közben ne kerülhessenek ki példányok, ill ne legyenek duplikálva esetleg egymást felülírva
 * 			- Adatbázisok hasíthatósága sok adatkapcsolat de csak egy gyorsítótár
 * 			- adatbázis alpéldány, selectkor klónozva legyen minden, update-kor megfelelően és konzisztensen legyenek a példényok tárolva, publikálva. 			
 * 
 * 
 * */
public abstract class GenericStoreDatabase implements Closeable, ExternalDataAttached
{
	protected static final boolean USE_BULK_LOAD = true;
	
	/**
	 * Gyorsítótárazás esetén visszadhatja ez is, elsőként ez lesz meghívva.
	 * Ha nem null akkor további teendő nincs, ez lesz visszaadva.
	 * */
	protected Map<Long,Reference<GenericStorable>> cache = new ConcurrentHashMap<>();
	
	protected long modificationCount = 0;
	
	protected long lastId = 0;
	
	public long getModificationCount()
	{
		return modificationCount;
	}
	
	protected static final GetBy1<Map<Long,GsdbModelPlacementRequests>, Class> CREATE_MAP = (GetBy1) GetByTools.wrapSimpleGet(SimpleGetFactory.getHashMapFactory());
	
	protected Map<Class, Map<Long,GsdbModelPlacementRequests>> bulkPlacementRequests = new HashMap<>();
	
	void putPlacementRequest(Class c, long id, SimplePublish1<GenericStorable> placer) throws Exception
	{
		if(!USE_BULK_LOAD)
		{
			getSingleObjectByID(id, getDescendantClassesFor(c));
		}
		
		Reference<GenericStorable> p = cache.get(id);
		if(null != p)
		{
			GenericStorable gs = p.get();
			if(null != gs)
			{
				if(NULL_VALUE == gs)
				{
					gs = null;
				}
				
				placer.publish(gs);
				return;
			}
		}
		
		Map<Long, GsdbModelPlacementRequests> add = MapTools.getOrCreate(bulkPlacementRequests, c, CREATE_MAP);
		GsdbModelPlacementRequests append = add.get(id);
		if(null == append)
		{
			add.put(id, append = new GsdbModelPlacementRequests(c, id));
		}
		
		append.placers.add(placer);
	}
	
	protected void resolvePlacements() throws IOException, Exception
	{
		if(bulkPlacementRequests.isEmpty())
		{
			return;
		}
		BulkTransitMap<Class, Map<Long, GsdbModelPlacementRequests>> reqs = new BulkTransitMap<>();
		reqs.putAll(bulkPlacementRequests);
		bulkPlacementRequests.clear();
		
		ArrayList<GenericStorable> tmp = new ArrayList<GenericStorable>();
		
		for(Entry<Class, Map<Long,GsdbModelPlacementRequests>> kv:reqs.entrySet())
		{
			Map<Long, GsdbModelPlacementRequests> ids = kv.getValue();
			getAllObjectsByQuery(kv.getKey(), F.in.is("do", ids.keySet()), tmp);
			//GenericStorage.getAllObjectsByQuery(this);
			
			for(GenericStorable t:tmp)
			{
				long id = GenericStorage.getID(t);
				GsdbModelPlacementRequests req = ids.get(id);
				if(null != req)
				{
					for(SimplePublish1<GenericStorable> p:req.placers)
					{
						p.publish(t);
					}
				}
			}
		}
		
		resolvePlacements();
	}
	
	//protected Map<Class<? extends GenericStorable>,Class<? extends GenericStorable>[]> storedClasses = null;
	protected MultiMap<Class<? extends GenericStorable>,Class<? extends GenericStorable>> storedClasses = null;
	
	public static FieldData[] getOrCreateFieldData(Class<? extends GenericStorable> cls) throws InstantiationException, IllegalAccessException
	{
		return GenericStorage.getOrCollectClassData(cls);
	}
	
	public static class HardReference<T> extends SoftReference<T>
	{
		final T obj;
		public HardReference(T referent)
		{
			super(referent);
			obj = referent;
		}
	}
	
	protected static Reference<GenericStorable> refObject(GenericStorable gs)
	{
		return new HardReference<GenericStorable>(gs);
	}
	
	public static final GenericStorable NULL_VALUE = new GenericStorable()
	{
		@Override public void setGenericStoreData(GenericStoreData data) {}
		@Override public Map<String, GenericStoreMode> getSelfDefinedMapping() {return null;}
		@Override public GenericStoreData getGenericStoreData() {return null;}
		@Override public void beforeStored(GenericStoreDatabase db) {}
		@Override public void afterRestored(GenericStoreDatabase db) {}
	};
	
	protected GenericStorable getFromCacheOrPublishRead(long id, Object res, Class<GenericStorable> re, String cls, FieldData[] fds) throws Exception
	{
		Reference<GenericStorable> ref = cache.get(id);
		GenericStorable ret = null;
		if(ref != null)
		{
			ret = ref.get();
			if(ret == NULL_VALUE)
			{
				return null;
			}
			
			if(ret != null)
			{
				return ret;
			}
		}
		
		ret = GenericStorage.newInstance(re);
		
		try
		{
			cache.put(id, ref = refObject(ret));
			setupReadedObject(ret, id);
			ret = getReader(cls).readObject(id, res, ret, this, fds);
			if(ret != null)
			{
				ret.afterRestored(this);
			}
			else
			{
				ret = NULL_VALUE;
			}
			
			return ret;
		}
		catch(Exception e)
		{
			cache.remove(id);
			throw e;
		}
	}
	
	//TODO ha megvan a gyorsítótárba akkor az az átadott egy példány eldobásra kerül, majd itt hívjuk a newInstance-t!
	protected GenericStorable _getFromCacheOrPublishRead(long id,Object res,GenericStorable re,String cls,FieldData[] fds) throws Exception
	{
		Reference<GenericStorable> ref = cache.get(id);
		GenericStorable ret = null;
		if(ref != null)
		{
			ret = ref.get();
			if(ret == NULL_VALUE)
			{
				return null;
			}
			
			if(ret != null)
			{
				return ret;
			}
		}
		
		cache.put(id, refObject(re));
		
		try
		{
			setupReadedObject(re, id);
			ret = getReader(cls).readObject(id, res, re, this, fds);
			if(ret != null)
				ret.afterRestored(this);
			return ret;
		}
		catch(Exception e)
		{
			cache.remove(id);
			throw e;
		}
	}
	
	protected void setupReadedObject(GenericStorable gs,long id)
	{
		GenericStoreData dat = new GenericStoreData();
		dat.id = id;
		dat.state = GenericStorageObjectState.PERSISTED;
		dat.owner = this;
		
		gs.setGenericStoreData(dat);
	}
	
	protected GenericStorable getSingleObjectByID(long id, List<Class<? extends GenericStorable>> clss) throws Exception
	{
		Reference<GenericStorable> ref = cache.get(id);
		GenericStorable ret = ref==null?null:ref.get();
		
		if(NULL_VALUE == ret)
		{
			return null;
		}
		
		if(ret == null)
		{
			//publish, ha hiba lép fel akkor pedig törlés
			for(Class<? extends GenericStorable> c:clss)
			{
				try(GenericStoreQueryResult res = getIDListByQuery(c, F.eq.is("do", id), true))
				{
					if(res == null)
						continue;
					
					GenericStoreDataReader reader = getReader(c.getName());
					
					for(ResultUnit ru:res.getResults())
					{
						Closeable re = ru.getCursor();
						if(!reader.setReadyToRead(re))
							continue;
						
						ret = GenericStorage.newInstance(c);
						cache.put(id, refObject(ret));
						try
						{
							setupReadedObject(ret, id);
							ret = reader.readObject(id, re, ret, this, GenericStorage.getOrCollectClassData(c));
							if(ret != null)
								ret.afterRestored(this);
							return ret;
						}
						catch(Exception e)
						{
							cache.remove(id);
							throw e;
						}
					}
				}
			}
		}
		
		if(null == ret)
		{
			cache.put(id, refObject(NULL_VALUE));
		}
		
		resolvePlacements();
		
		return ret;
	}
	
	public List<Class<? extends GenericStorable>> getDescendantClassesFor(Class<? extends GenericStorable> cls) throws Exception
	{
		List<Class<? extends GenericStorable>> ret = storedClasses.getList(cls);
		if(null == ret)
		{
			return NullList.instance;
		}
		return Collections.unmodifiableList(ret); 
	}
	
	//TODO ha új osztály kerül eltárolásra akkor "fedezd fel" a leszármazottjait és ha azok tárolva vannak add hozzá a célosztályok leszármazottjaihoz
	public void mustCallAfterConnectionEstablishedBeforeUse() throws GenericStoreException 
	{
		try
		{
			resfreshStoredClassesRegister();
			lastId = getCurrentId();
		}
		catch (Exception e)
		{
			throw new GenericStoreException(e);
		}
	}
	
	public boolean isStored(Class<? extends GenericStorable> cls) throws Exception
	{
		String c = cls.getName();
		
		if(storedClasses.containsKey(cls))
		{
			return true;
		}
		
		//resfreshStoredClassesRegister();
		
		for(String s:listStoredClasses())
		{
			if(c.equals(s))
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean isStored(Class<? extends GenericStorable> cls,String[] lst)
	{
		String n = cls.getName();
		for(String s:lst)
		{
			if(s.equals(n))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Ez egy fontos mechanizmus része. Ahogy a java így a GenericStorage is követi a tipusinformációkat, ha egy mező pl.: GenericStorable akkor
	 * az adatbázisban ezen osztály leszármazottjaihoz tartozó táblák végignézésre kerülnek.
	 * Ehhez az öröklési láncban azoknak az osztályoknak is szerepelnie kell
	 * amelyhez amúgy nem jött létre tároló tábla.
	 * 
	 * Az egyes bejegyzések (osztályok) a lehetséges leszármazottakra mutatnak.
	 * //TODO ez így nem lesz jó, fordítva lenne célszerű bejárni a fát, ha rákérdeznek egy interface-re az nem szerepel a hierarhiában 
	 * */
	protected synchronized void recursiveIntoMapIfStored
	(
		Class<? extends GenericStorable> rootcls,
		Class<? extends GenericStorable> curr,
		String[] storeds,
		MultiMap<Class<? extends GenericStorable>, Class<? extends GenericStorable>> map,
		ClassLoader ctxcldr
	)
	{
		if(rootcls.isInterface() || Modifier.isAbstract(rootcls.getModifiers()))
		{
			return;
		}
		
		map.put(curr, rootcls);
		
		if(curr.equals(GenericStorable.class)) // ez a tárolhatók gyökere?
		{
			return;
		}
		
		sup:
		{
			Class<?> c = curr.getSuperclass();
			if(c == null)
			{
				break sup;
			}
			
			if(GenericStorable.class.isAssignableFrom(c))
			{
				recursiveIntoMapIfStored(rootcls, (Class<? extends GenericStorable>)c, storeds, map, ctxcldr);
			}
		}
		
		for(Class<?> c:curr.getInterfaces())
		{
			if(GenericStorable.class.isAssignableFrom(c))
			{
				recursiveIntoMapIfStored(rootcls, (Class<? extends GenericStorable>)c, storeds, map, ctxcldr);
			}
		}
	}

	public abstract GenericStoreDataWriter getWriter(String cls) throws Exception;
	
	public abstract String getDatabaseName();
	
	public abstract GenericStoreQueryResult getIDListByQuery(Class<? extends GenericStorable> cls,LogicalGroup lg,boolean all_field) throws Exception;

	//public abstract GenericStoreQueryResult getIDListByNativeStringQuery(Class<? extends GenericStorable> cls,String str) throws Exception;
	
	public abstract void createStorageForClass(Class<? extends GenericStorable> cls,FieldData[] data) throws Exception;
	
	//TODO ez elé regisztrálni hogy új osztály került nyilvátartásra
	public void createStorageForClassIfNeeded(Class<? extends GenericStorable> cls,FieldData[] data) throws Exception
	{
		String[] clss = listStoredClasses();
		synchronized (storedClasses)
		{
			if(isStored(cls, clss))
			{
				//TODO mező változás csekkolása
				return;
			}
			
			createStorageForClass(cls, data);
			resfreshStoredClassesRegister();
			//recursiveIntoMapIfStored(cls, cls, clss, storedClasses, Thread.currentThread().getContextClassLoader());
		}
	}
	
	public void dropClassStorage(Class<? extends GenericStorable> cls) throws Exception//TODO törlés az adatbázisból és nyilvántartásból
	{//TODO leszármazottak?
		++modificationCount;
		dropClassStorageImpl(cls);
		resfreshStoredClassesRegister();
	}
	
	protected abstract void dropClassStorageImpl(Class<? extends GenericStorable> cls) throws Exception;
	
	//public abstract void deleteObjectByIDAndPossibleClasses(long id,Class<? extends GenericStorable>[] cls) throws Exception;
	
	public abstract void deleteObjectByIDSByClass(long[] id, Class<? extends GenericStorable>[] cls) throws Exception;
	
	public abstract String[] listStoredClasses() throws Exception;
	
	public abstract GenericStoreDataReader getReader(String clas) throws Exception;
	
	public void resfreshStoredClassesRegister() throws Exception
	{
		storedClasses = new MultiMap();
		String[] storeds = listStoredClasses();
		
		HashMap<Class<? extends GenericStorable>,ArrayList<Class<? extends GenericStorable>>> clsMap = new HashMap<>();
		
		ClassLoader clsldr = Thread.currentThread().getContextClassLoader();
		
		for(String strcls:storeds)
		{
			try
			{
				Class<?extends GenericStorable> cls =  (Class<? extends GenericStorable>) clsldr.loadClass(strcls);
				if(isStored(cls, storeds))
					recursiveIntoMapIfStored(cls, cls, storeds, storedClasses, clsldr);
			}
			catch(Exception e)
			{}
		}
	}
	
	public void fillResults(Collection coll, ResultUnit result) throws Exception
	{
		Object rs = result.getCursor();
		Class cls = Thread.currentThread().getContextClassLoader().loadClass(result.getReturnClass());
		FieldData[] fds = getOrCreateFieldData(cls);
		GenericStoreDataReader reader = getReader(result.getReturnClass()); 
		while(reader.nextResult(rs))//TODO ID beállítása és gyorsítótárazás
		{
			GenericStorable add = getFromCacheOrPublishRead(reader.getIdBy(rs), rs, cls, result.getReturnClass(), fds);
			if(null != add)
			{
				coll.add(add);
			}
		}
		
		resolvePlacements();
	}
	
	protected static Set<? extends GenericStorable> extract(Collection<? extends GenericStorable> src) throws InstantiationException, IllegalAccessException
	{
		Set<GenericStorable> ret = new HashSet<>();
		for(GenericStorable s:src)
		{
			extractInto(ret, s);
		}
		
		return ret;
	}
	
	protected static void extractInto(Set<GenericStorable> dst, GenericStorable gs) throws InstantiationException, IllegalAccessException
	{
		if(isNeedToStoreReal(gs) && dst.add(gs))
		{
			FieldData[] fd = getOrCreateFieldData(gs.getClass());
			
			//TODO FieldData[] => mapping data that holds cached stuf slike this (other GenericStorable fields)
			
			//discover and add
			for(FieldData f:fd)
			{
				if(GenericStoreDataType.GenericDataId == f.type)
				{
					Object o = f.f.get(gs);
					if(o instanceof GenericStorable)
					{
						extractInto(dst, (GenericStorable) o);
					}
				}
				else if(GenericStoreDataType.Array == f.type)
				{
					Object[] os = (Object[]) f.f.get(gs);
					if(null != os)
					{
						for(Object o:os)
						{
							if(o instanceof GenericStorable)
							{
								extractInto(dst, (GenericStorable)o);
							}
						}
					}
				}
			}
		}
	}
	
	public static boolean isNeedToStoreReal(GenericStorable gs)
	{
		if(null != gs)
		{
			GenericStoreData data = gs.getGenericStoreData();
			if(null == data)
			{
				return true;
			}
			
			if(gs.getGenericStoreData().id == -1 || (GenericStorageObjectState.UNDER_SAVE != data.state && gs.getGenericStoreData().isModified()))
			{
				return true;
			}
		}
		return false;
	}
	
	public void storeAll(Collection<? extends GenericStorable> _coll) throws Exception
	{
		Collection<? extends GenericStorable> coll = extract(_coll);
		//TODO collect all referenced element and save that way (so no indirect save applied in that case)
		
		Map<Class<? extends GenericStorable>,List<? extends GenericStorable>> map = new SmallMap<>();
		
		int numz = 0;
		
		for(GenericStorable gs:coll)
		{
			gs.beforeStored(this);
			((List)MapTools.ensureKey(map, gs.getClass(), (SimpleGet) SimpleGetFactory.getArrayListFactory())).add(gs);
			if(GenericStorage.getID(gs) == -1)
			{
				++numz;
			}
		}
		
		++modificationCount;
		
		for(GenericStorable o:coll)
		{
			getOrCreateGenericStoreData(o).state = GenericStorageObjectState.UNDER_SAVE;
		}
		
		try
		{
			List<Long> ids = reserveNextIDRangeAtomic(numz);
			numz = 0;
			
			ArrayList<Long> news = new ArrayList<>();
			
			for(GenericStorable gs:coll)
			{
				if(GenericStorage.getID(gs) == -1)
				{
					GenericStoreData data = GenericStorage.getOrCreateGenericStoreData(gs);
					data.setOwnerDatabase(this);
					long id = ids.get(numz++);
					news.add(id);
					data.setId(id);
					cache.put(id, refObject(gs));
				}
			}
			
			try
			{
				storeAll(map);
			}
			catch(Exception e)
			{
				for(Long i:news)
				{
					cache.remove(i);
				}
				throw e;
			}
		}
		catch(Exception e)
		{
			for(GenericStorable o:coll)
			{
				o.getGenericStoreData().state = GenericStorageObjectState.MODIFIED;
			}	
			throw e;
		}
		
		for(GenericStorable o:coll)
		{
			o.getGenericStoreData().state = GenericStorageObjectState.PERSISTED;
		}
	}
	
	protected static <T> T getOfType(Object[] os, Class<T> c)
	{
		for(Object o:os)
		{
			if(c.isAssignableFrom(o.getClass()))
			{
				return (T) o;
			}
		}
		
		return null;
	}
	
	protected <T extends GenericStorable> void getAllObjectsByQuery(Class<? extends T> cls,LogicalGroup lg,Collection<T> coll, GsdbExtraCaluse... extra) throws Exception
	{
		ArrayList<T> tmp = null;
		Collection<T> dst = null == extra || extra.length == 0?coll:(tmp = new ArrayList<>());
		
		for(Class<? extends GenericStorable> c:getDescendantClassesFor(cls))
		{
			if(!isStored(c))
				return;
		
			try(GenericStoreQueryResult res = getIDListByQuery(c, lg, true))
			{
				for(ResultUnit u:res.getResults())
				{
					fillResults(dst, u);
				}
			}
		}
		resolvePlacements();
		
		
		if(null == tmp)
		{
			return;
		}
		
		
		OrderBy ob = getOfType(extra, OrderBy.class);
		Limit l = getOfType(extra, Limit.class);
		Offset o = getOfType(extra, Offset.class);
		
		if(null != ob)
		{
			Comparator<GenericStorable> cmp = ob.createComparator(cls);
			if(null != cmp)
			{
				Collections.sort(tmp, cmp);
			}
		}
		
		int off = 0;
		int limit = Integer.MAX_VALUE;
		
		if(null != o)
		{
			off = o.offset;
		}
		
		if(null != l)
		{
			limit = l.limit;
		}
		
		int end = off+limit;
		if(end > tmp.size())
		{
			end = tmp.size();
		}
		
		for(int i=off;i<end;++i)
		{
			coll.add(tmp.get(i));
		}
	}
	
	protected GenericStoreData getOrCreateGenericStoreData(GenericStorable gs)
	{
		GenericStoreData ret = gs.getGenericStoreData();
		if(null == ret)
		{
			gs.setGenericStoreData(ret = new GenericStoreData());
			ret.id = -1;
			ret.state = GenericStorageObjectState.NEW;
			ret.owner = this;
		}
		return ret;
	}
	
	protected void store(GenericStorable gs) throws Exception
	{
		storeAll(new OneShotList<>(gs));
	}
	
	protected abstract void storeAll(Map<Class<? extends GenericStorable>, List<? extends GenericStorable>> map) throws Exception;
	
	/**
	 * Mindig a következő, nem foglalat, egyedi azonosítót adja vissza.
	 * Az implementáció legyen atomi vagy szálbiztos
	 * */
	/*protected abstract long reserveNextID() throws Exception;
	
	public long assignNextId() throws Exception
	{
		return lastId = reserveNextID();
	}*/
	
	protected abstract List<Long> reserveNextIDRangeAtomic(int size) throws Exception;
	
	public List<Long> assignNextIdRange(int size) throws Exception
	{
		List<Long> ret = reserveNextIDRangeAtomic(size);
		Long max = MathTools.getMaxLong(ret);
		if(null != max && max > lastId)
		{
			lastId = max;
		}
		return ret;
	}
	
	protected abstract long getCurrentId() throws Exception;
	
	//protected abstract Closeable findObjectByIdAndClass(long id,String cls) throws Exception;
	
	public abstract void close();
	
	protected transient Map<String, Object> extraData;
	
	@Override
	public Map<String, Object> getExtraDataMap()
	{
		if(null == extraData)
		{
			extraData = new SmallMap<>();
		}
		return extraData;
	}
	
	//TODO commitAll
	//TODO refresh
	//TODO hooks before touch table classes (update last modify time)
}