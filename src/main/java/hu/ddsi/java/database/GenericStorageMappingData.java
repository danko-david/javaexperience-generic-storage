package hu.ddsi.java.database;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.javaexperience.reflect.Mirror;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreNative;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreSerialized;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreStorableID;

public class GenericStorageMappingData
{
	public final Class cls;
	public final FieldData[] allFd;
	public final FieldData[] storedFd;
	
	protected GenericStorageMappingData(Class cls, FieldData[] fds)
	{
		this.cls = cls;
		this.allFd = fds;
		
		ArrayList<FieldData> fs = new ArrayList<>();
		for(FieldData fd: fds)
		{
			if(fd.isStored())
			{
				fs.add(fd);
			}
		}
		
		this.storedFd = fs.toArray(emptyFDA);
	}
	
	public static final FieldData[] emptyFDA = new FieldData[0];
	
	protected static Map<Class<? extends GenericStorable>, GenericStorageMappingData> classesFieldDatas = new ConcurrentHashMap<>();
	static
	{
		classesFieldDatas.put(GenericStorable.class, new GenericStorageMappingData(GenericStorable.class, emptyFDA));
	}
	
	protected static Map<String,Class<?>> stringToClass = new HashMap<>();
	
	protected static boolean isNeedToStore(Field f)
	{
		return !(Modifier.isStatic(f.getModifiers()) || dontStoreByAnnotation(f));
	}

	protected static boolean dontStoreByAnnotation(Field f)
	{
		//TeaVm might return null
		Annotation[] anns = f.getAnnotations();
		
		if(null != anns)
		{
			for(Annotation a:anns)
			{
				if(a instanceof GenericStoreIgnore)
					return true;
				else if(a instanceof GenericStoreNative)
					return false;
				else if(a instanceof GenericStoreStorableID)
					return false;
				else if(a instanceof GenericStoreSerialized)
					return false;
			}
		}
		return false;
	}
	
	public static GenericStorageMappingData getOrCollectAllClassData(Class<? extends GenericStorable> cls)
	{
		GenericStorageMappingData ret = classesFieldDatas.get(cls);
		if(ret != null)
		{
			return ret;
		}
		
		GenericStorable instance = null;
		
		if(!cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
		{
			try
			{
				instance = GenericStorage.newInstance(cls);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return getOrCollectClassData(cls, instance);
	}
	
	protected static FieldData[] getOrCollectClassData(Class cls) throws InstantiationException, IllegalAccessException
	{
		return getOrCollectAllClassData(cls).storedFd;
	}
	
	protected static GenericStorageMappingData getOrCollectClassData(Class cls, GenericStorable obj)
	{
		if(null != obj)
		{
			cls = obj.getClass();
		}

		GenericStorageMappingData data = classesFieldDatas.get(cls);
		
		if(data != null)
		{
			return data;
		}
		
		ArrayList<FieldData> fds = new ArrayList<>();
		Map<String,GenericStoreMode> map = null;
		
		if(null != obj)
		{
			map = obj.getSelfDefinedMapping();
		}
		
		ArrayList<Field> tmp = new ArrayList<>();
		Mirror.collectClassFields(cls, tmp, false);

		
		if(map != null)
		{
			GenericStoreMode def = map.get(null);
			if(def == null)
				def = GenericStoreMode.GenericStoreDont;
				
			for(Field f:tmp)
			{
				if(Modifier.isStatic(f.getModifiers()))
				{
					continue;
				}
				
				GenericStoreMode mod = map.get(f.getName());
				
				if(!isNeedToStore(f))
				{
					mod = GenericStoreMode.GenericStoreDont;
				}
				
				if(mod == null)
					mod = def;
				
				FieldData fd = new FieldData(f,mod);
				if(fd.getDataType() == null)
					continue;
				
				addOverwrite(fd, fds);
			}
		}
		else
		{
			for(Field f:tmp)
			{
				if(Modifier.isStatic(f.getModifiers()))
				{
					continue;
				}
				
				FieldData fd = new FieldData(f);
				if(fd.getDataType() == null)
					continue;
				
				addOverwrite(fd, fds);
			}
		}
		
		data = new GenericStorageMappingData(cls, fds.toArray(emptyFDA));
		
		classesFieldDatas.put(cls,data);
		stringToClass.put(cls.getName(), cls);
		
		return data;
	}
	
	protected static void addOverwrite(FieldData data, ArrayList<FieldData> fds)
	{
		String name = data.f.getName();
		for(int i=0;i<fds.size();++i)
		{
			FieldData d = fds.get(i);
			if(d.f.getName().equals(name))
			{
				//fds.set(i, data);
				return;
			}			
		}
		
		fds.add(data);
	}
}
