package hu.ddsi.java.database;

import java.lang.reflect.Field;

import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;

public class GenericStoreDataArray implements ObjectWithProperty
{
	final GenericStoreDataType primitivType;
	int dimensions;
	
	public GenericStoreDataArray(GenericStoreDataType primitiveType,int dimension)
	{
		this.primitivType = primitiveType;
		this.dimensions = dimension;
	}
	
	public GenericStoreDataType getDataType()
	{
		return primitivType;
	}
	
	public int getDimensionNumber()
	{
		return dimensions;
	}
	
	public static GenericStoreDataArray determineGenericStoreData(Field f)
	{
		return determineGenericStoreData(f, f.getType(),0);
	}
	
	private static GenericStoreDataArray determineGenericStoreData(Field f,Class<?> cls,int level)
	{
		if(cls.isArray())
			return determineGenericStoreData(f, cls.getComponentType(), level+1);

		return new GenericStoreDataArray(GenericStoreDataType.getStoreType(cls,f.getAnnotations()), level);
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
	
	protected static ObjectWithPropertyStorage<GenericStoreDataArray> PROPS = new ObjectWithPropertyStorage<>();
	
	static
	{
		PROPS.addExaminer("dimensions", (e)->e.dimensions);
		PROPS.addExaminer("storageType", (e)->e.primitivType);
	}
}