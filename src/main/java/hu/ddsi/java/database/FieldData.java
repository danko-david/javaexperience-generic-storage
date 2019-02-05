package hu.ddsi.java.database;

import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;

import java.lang.reflect.Field;
import java.util.Map;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.interfaces.ExternalDataAttached;
import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;

public class FieldData implements ExternalDataAttached, ObjectWithProperty
{
	public final Field f;
	public final GenericStoreDataType type;
	public final GenericStoreDataArray arr;
	public final int maxSize = 1000;
	
	public FieldData(Field f)
	{
		setFieldAccessible(this.f = f);
		GenericStoreDataType gtt = null;
		
		try
		{
			gtt = GenericStoreDataType.getStoreType(f);
		}
		catch(Exception e)
		{
			throw new RuntimeException("On field: "+f, e);
		}
		type = gtt;
		arr = type == GenericStoreDataType.Array? GenericStoreDataArray.determineGenericStoreData(f):null;
	}
	
	public FieldData(Field f, GenericStoreMode gsm)
	{
		setFieldAccessible(this.f = f);
		if(GenericStoreMode.GenericStoreDont.equals(gsm))
		{
			type = null;
			arr = null;
		}
		else if(GenericStoreMode.GeneriStoreNative.equals(gsm))
		{
			GenericStoreDataType gtt = GenericStoreDataType.getStoreType(f);
			type = gtt;
			arr = type == GenericStoreDataType.Array? GenericStoreDataArray.determineGenericStoreData(f):null;
		}
		else
		{
			type = gsm.getGenericStoreDataType();
			arr = null;
		}
	}
	
	private static final Field modifiersField = modifierFields();
	
	private static Field modifierFields() //TODO ez mennyire biztos?
	{
		Field f = null;
		try
		{
			f = Field.class.getDeclaredField("modifiers");
			f.setAccessible(true);
		}
		catch (Exception e) {}
	    return f;
	}
	
	private static void setFieldAccessible(Field field)
	{
		try
		{
	      field.setAccessible(true);
	      //modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}catch(Throwable t)
		{}
	}

	
	public boolean isArray()
	{
		return type == GenericStoreDataType.Array;
	}
	
	public Field getField()
	{
		return f;
	}
	
	public GenericStoreDataType getDataType()
	{
		return type;
	}
	
	public GenericStoreDataArray getArrayData()
	{
		return arr;
	}

	//TODO read from annotations
	public int getMaxSize()
	{
		return maxSize;
	}
	
	@GenericStoreIgnore
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
	
	public boolean isStored()
	{
		return GenericStoreDataType.DontStore != type;
	}
	
	protected static ObjectWithPropertyStorage<FieldData> PROPS = new ObjectWithPropertyStorage<>();
	
	static
	{
		PROPS.addExaminer("type", (e)-> new WrappedClassData(e.f.getType()));
		PROPS.addExaminer("fieldName", (e)-> e.f.getName());
		PROPS.addExaminer("fieldStorageType", (e)-> e.type);
		PROPS.addExaminer("maxSize", (e)-> e.maxSize);
		PROPS.addExaminer("extraData", (e)-> e.getExtraDataMap());
		PROPS.addExaminer("arrayStorage", (e)-> e.getExtraDataMap());
	}
}