package hu.ddsi.java.database;

import java.lang.reflect.Field;
import java.util.Comparator;

import eu.javaexperience.collection.ComparatorTools;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;

public class OrderBy implements GsdbExtraCaluse
{
	@Override
	public GsdbExtraClauseType getType()
	{
		return GsdbExtraClauseType.order;
	}
	
	public final boolean asc;
	public final String field;
	
	public OrderBy(String field, boolean asc)
	{
		this.field = field;
		this.asc = asc;
	}
	
	public static OrderBy asc(String field)
	{
		return new OrderBy(field, true);
	}
	
	public static OrderBy desc(String field)
	{
		return new OrderBy(field, false);
	}
	
	protected static Object examine(Field f, Object subject)
	{
		if(null == subject)
		{
			return null;
		}
		
		try
		{
			return f.get(subject);
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public static final Comparator<GenericStorable> COMPARATOR_FOR_GS = (a,b)-> ComparatorTools.COMPARATOR_FOR_LONG.compare(GenericStorage.getID(a), GenericStorage.getID(b));
	
	public Comparator<GenericStorable> createComparator(Class c) throws InstantiationException, IllegalAccessException
	{
		FieldData fd = GenericStorage.getFieldByName(GenericStorage.getOrCollectClassData(c), field);
		if(null == fd)
		{
			throw new RuntimeException("No field `"+field+"` in class: "+c.getName());
		}
		
		Field f = fd.f;
		
		Class type = f.getType();
		
		Comparator cmp = null;
		
		if(type.isEnum())
		{
			cmp = ComparatorTools.COMPARATOR_FOR_ENUM;
		}
		else if(Comparator.class.isAssignableFrom(c))
		{
			cmp = (Comparator) c.newInstance();
		}
		else if(GenericStorable.class.isAssignableFrom(type))
		{
			cmp = COMPARATOR_FOR_GS;
		}
		else
		{
			cmp = ComparatorTools.getComparatorByClass(type);
		}
		
		if(null == cmp)
		{
			cmp = ComparatorTools.COMPARATOR_FOR_OBJECT_HASH;
		}
		
		Comparator ret = ComparatorTools.createFieldComparatorWithNulls
		(
			new GetBy1<Object, GenericStorable>()
			{
				@Override
				public Object getBy(GenericStorable a)
				{
					return examine(f, a);
				}
			},
			cmp,
			true
		);
		
		if(!asc)
		{
			return ComparatorTools.reverseOrder(ret);
		}
		
		return ret;
	}
}
