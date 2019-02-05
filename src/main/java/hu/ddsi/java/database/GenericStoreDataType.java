package hu.ddsi.java.database;

import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreNative;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreSerialized;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreStorableID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import eu.javaexperience.exceptions.UnimplementedCaseException;

public enum GenericStoreDataType
{
	DontStore(void.class,SimpleDataType.dontStore),
	Boolean(boolean.class,SimpleDataType.sint32),
	Byte(byte.class,SimpleDataType.sint32),
	Char(char.class,SimpleDataType.string),
	Short(short.class,SimpleDataType.sint32),
	Integer(int.class,SimpleDataType.sint32),
	
	Enum(int.class,SimpleDataType.sint32),
	
	Float(float.class,SimpleDataType.doublefloat64),
	Long(long.class,SimpleDataType.slongint64),
	Double(double.class,SimpleDataType.doublefloat64),
	
	Date(Long.class,SimpleDataType.slongint64),
	
	String(String.class,SimpleDataType.string),
	
	/**
	 * Mező amit szintúgy lementjük az adatbázisba, mint a befogadó osztályt.
	 * */
	GenericDataId(long.class,SimpleDataType.slongint64),
	
	/**
	 * A mezőt sorosítva tároljuk.
	 * */
	Serialized(byte[].class,SimpleDataType.blob),
	
	Array(GenericStoreDataArray.class,SimpleDataType.array),
	
	;
	
	private final Class<?> javaType;
	
	private final SimpleDataType simpleType;
	
	public static enum SimpleDataType
	{
		dontStore,
		sint32,
		slongint64,
		string,
		blob,
		array,
		doublefloat64,
	}
	
	public Class<?> getJavaTransferClass()
	{
		return javaType;
	}

	private GenericStoreDataType(Class<?> transferType,SimpleDataType simple)
	{
		this.javaType = transferType;
		this.simpleType = simple;
	}
	
	public static GenericStoreDataType getStoreType(Field f)
	{
		boolean throwExcpetion = true;
		Annotation[] ann = f.getAnnotations();
		//at this point only TeaVM goes into this if 
		if(null == ann)
		{
			throwExcpetion = false;
			if(Modifier.isTransient(f.getModifiers()))
			{
				return DontStore;
			}
		}
		GenericStoreDataType ret = getStoreType(f.getType(), ann, throwExcpetion);
		if(null == ret)
		{
			return DontStore;
		}
		
		return ret;
	}
	
	public static GenericStoreDataType getStoreType(Class<?> t, Annotation[] anns)
	{
		return getStoreType(t, anns, true);
	}
	
	public static GenericStoreDataType getStoreType(Class<?> t, Annotation[] anns, boolean throwException)
	{
		boolean store = false;
		if(null != anns)
		{
			for(Annotation a:anns)
			{
				if(a instanceof GenericStoreIgnore)
					return DontStore;
				else if(a instanceof GenericStoreStorableID)
				{
					if(t.isArray())
						return Array;
						else
							return GenericDataId;
				}
				else if(a instanceof GenericStoreSerialized)
					return Serialized;
				else if(a instanceof GenericStoreNative)
				{
					store = true;
					break;
				}
			}
		}
		
		if(t.isArray())
			return Array;
		
		if(boolean.class.equals(t) || Boolean.class.equals(t))
			return Boolean;
		
		if(byte.class.equals(t) || Byte.class.equals(t))
			return Byte;

		if(char.class.equals(t) || Character.class.equals(t))
			return Char;
		
		if(short.class.equals(t) || Short.class.equals(t))
			return Short;
		
		if(int.class.equals(t) || Integer.class.equals(t))
			return Integer;
		
		if(long.class.equals(t) || Long.class.equals(t))
			return Long;
		
		if(float.class.equals(t) || Float.class.equals(t))
			return Float;
		
		if(double.class.equals(t) || Double.class.equals(t))
			return Double;
		
		if(String.class.equals(t))
			return String;
		
		if(java.util.Date.class.equals(t))
			return Date;
		
		if(t.isEnum())
			return Enum;
		
		if(GenericStorable.class.isAssignableFrom(t))
			return GenericDataId;
		
		if(store)
		{
			return Serialized;
		}
		
		if(GenericStoreData.class == t)
		{
			return DontStore;
		}
		
		if(throwException)
		{
			throw new UnimplementedCaseException("Case for class:"+t+(null == t?"":" ("+t.getName()+")")+" is unimplemented");
		}
		
		return null;
	}
}