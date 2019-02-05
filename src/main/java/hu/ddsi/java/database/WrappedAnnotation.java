package hu.ddsi.java.database;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.ClassData;

public class WrappedAnnotation implements ObjectWithProperty
{
	public static List<WrappedAnnotation> wrap(Annotation[] annotations)
	{
		List<WrappedAnnotation> ann = new ArrayList<>();
		for(Annotation a:annotations)
		{
			ann.add(wrap(a));
		}
		return ann;
	}
	
	public static WrappedAnnotation wrap(Annotation ann)
	{
		WrappedAnnotation ret = new WrappedAnnotation();
		ret.props.put("annotationType", ann.annotationType().getName());
		ClassData cd = Mirror.getClassData(ann.annotationType());
		for(Method m:cd.getSelfMethods())
		{
			if(0 == m.getParameters().length)
			{
				try
				{
					Object wrap = m.invoke(ann);
					if(wrap instanceof Annotation)
					{
						wrap = wrap((Annotation) wrap);
					}
					else if(wrap instanceof Annotation[])
					{
						wrap = wrap((Annotation[]) wrap);
					}
					else if(wrap instanceof Class)
					{
						wrap = ((Class)wrap).getName();
					}
					
					ret.props.put(m.getName(), wrap);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} 
		return ret;
	}
	
	protected Map<String, Object> props = new SmallMap<>();

	@Override
	public Object get(String key)
	{
		return props.get(key);
	}

	@Override
	public String[] keys()
	{
		return props.keySet().toArray(Mirror.emptyStringArray);
	}
}
