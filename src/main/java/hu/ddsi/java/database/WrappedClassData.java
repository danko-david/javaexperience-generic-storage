package hu.ddsi.java.database;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.FieldSelectTools;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.semantic.references.MayNull;

public class WrappedClassData implements ObjectWithProperty
{
	protected Class type;
	public boolean shallow = false;
	protected GetBy1<Boolean, Field> fieldSelector;
	
	public WrappedClassData(Class<?> type)
	{
		this.type = type;
		fieldSelector = BoolFunctions.always();
	}

	public WrappedClassData(Class c, GetBy1<Boolean, Field> fieldSelector)
	{
		type = c;
		this.fieldSelector = fieldSelector;
	}

	@Override
	public Object get(String key)
	{
		return PROPS.get(this, key);
	}

	@Override
	public String[] keys()
	{
		return PROPS.keys();
	}
	
	public static @MayNull WrappedClassData wrap(Class c, GetBy1<Boolean, Field> fieldSelector)
	{
		if(null == c)
		{
			return null;
		}
		return new WrappedClassData(c, fieldSelector);
	}
	
	public static @MayNull WrappedClassData[] wrap(Class[] c, GetBy1<Boolean, Field> fieldSelector)
	{
		if(null == c)
		{
			return null;
		}
		
		WrappedClassData[] ret = new WrappedClassData[c.length];
		for(int i=0;i<ret.length;++i)
		{
			ret[i] = wrap(c[i], fieldSelector);
		}
		
		return ret;
	}
	
	protected Object extractSuperClass()
	{
		if(shallow)
		{
			Class cls = type.getSuperclass();
			if(null == cls)
			{
				return null;
			}
			return cls.getName();
		}
		else
		{
			return wrap(type.getSuperclass(), fieldSelector);
		}
	}
	
	protected Object extractInterface()
	{
		if(shallow)
		{
			Class[] cls = type.getInterfaces();
			if(null == cls)
			{
				return null;
			}
			String[] ret = new String[cls.length];
			int i = 0;
			for(Class c:cls)
			{
				ret[i++] = c.getName();
			}
			return ret;
		}
		else
		{
			return wrap(type.getInterfaces(), fieldSelector);
		}
	}
	
	protected static ObjectWithPropertyStorage<WrappedClassData> PROPS = new ObjectWithPropertyStorage<>();
	
	static
	{
		PROPS.addExaminer("name", (e)->e.type.getSimpleName());
		PROPS.addExaminer("fullName", (e)->e.type.getName());
		PROPS.addExaminer("superClass", (e)->e.extractSuperClass());
		PROPS.addExaminer("superInterfaces", (e)->e.extractInterface());
		PROPS.addExaminer("isArray", (e)->e.type.isArray());
		PROPS.addExaminer("isEnum", (e)->e.type.isEnum());
		PROPS.addExaminer("arrayComponent", (e)->wrap(e.type.getComponentType(), e.fieldSelector));
		PROPS.addExaminer("enums", (e)->e.type.getEnumConstants());
		PROPS.addExaminer("isInterface", (e)->e.type.isInterface());
		PROPS.addExaminer("isAbstract", (e)->Modifier.isAbstract(e.type.getModifiers()));
		PROPS.addExaminer("modifiers", (e)->e.type.getModifiers());
		PROPS.addExaminer("fields", (e)->WrappedFieldData.wrapAll(e.type.getFields(), e.fieldSelector));
		//TODO annontations
	}

	public static void discoverAndWrapShallow
	(
		Map<Class, WrappedClassData> map,
		Class c,
		boolean discoverFields
	)
	{
		if(null == c)
		{
			return;
		}
		
		if(!map.containsKey(c))
		{
			WrappedClassData wc = new WrappedClassData(c);
			wc.shallow = true;
			
			discoverAndWrapShallow(map, c.getSuperclass(), discoverFields);
			for(Class i:c.getInterfaces())
			{
				discoverAndWrapShallow(map, i, discoverFields);
			}
			
			if(discoverFields)
			{
				for(Field f:Mirror.getClassData(c).select(FieldSelectTools.SELECT_ALL_INSTANCE_FIELD))
				{
					discoverAndWrapShallow(map, f.getType(), discoverFields); 
				}
			}
		}
	}
}
