package hu.ddsi.java.database;

import java.util.Map;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.interfaces.ExternalDataAttached;

public class GsdbGenericExtraCaluse implements GsdbExtraCaluse, ExternalDataAttached
{
	public final GsdbExtraClauseType type;
	
	public GsdbGenericExtraCaluse()
	{
		type = null;
	}
	
	public GsdbGenericExtraCaluse(GsdbExtraClauseType type)
	{
		this.type = type;
	}
	
	@Override
	public GsdbExtraClauseType getType()
	{
		return type;
	}

	protected Map<String, Object> extraData;
	
	@Override
	public Map<String, Object> getExtraDataMap()
	{
		if(null == extraData)
		{
			extraData = new SmallMap<>();
		}
		return extraData;
	}
}
