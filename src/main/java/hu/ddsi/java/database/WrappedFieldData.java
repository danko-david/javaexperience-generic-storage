package hu.ddsi.java.database;

import java.lang.reflect.Field;
import java.util.Arrays;

import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;

public class WrappedFieldData implements ObjectWithProperty
{
	Field field;
	
	public WrappedFieldData(Field field)
	{
		this.field = field;
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
	
	protected static ObjectWithPropertyStorage<WrappedFieldData> PROPS = new ObjectWithPropertyStorage<>();
	
	static
	{
		PROPS.addExaminer("name", (f)->f.field.getName());
		PROPS.addExaminer("declaringClass", (f)->f.field.getDeclaringClass().getName());
		PROPS.addExaminer("type", (f)->f.field.getType().getName());
		PROPS.addExaminer("modifiers", (f)->f.field.getModifiers());
		PROPS.addExaminer("annotations", (f)->WrappedAnnotation.wrap(f.field.getAnnotations()));
	}
	
	public static WrappedFieldData wrap(Field field)
	{
		return new WrappedFieldData(field);
	}

	public static WrappedFieldData[] wrapAll(Field[] fields, GetBy1<Boolean, Field> fieldSelector)
	{
		WrappedFieldData[] ret = new WrappedFieldData[fields.length];
		int l = 0;
		for(int i = 0;i<ret.length;++i)
		{
			if(Boolean.TRUE == fieldSelector.getBy(fields[i]))
			{
				ret[l++] = wrap(fields[i]);
			}
		}
		
		if(l != ret.length)
		{
			ret = Arrays.copyOf(ret, l);
		}
		
		return ret;
	}
}
