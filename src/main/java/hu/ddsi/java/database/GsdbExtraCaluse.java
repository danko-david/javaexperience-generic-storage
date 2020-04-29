package hu.ddsi.java.database;

import eu.javaexperience.interfaces.ExternalDataAttached;

public interface GsdbExtraCaluse
{
	public GsdbExtraClauseType getType();
	
	public static enum WellKnownGsdbExtraCaluses implements GsdbExtraClauseType
	{
		order,
		offset,
		limit,
		group;

		@Override
		public String getType()
		{
			return name();
		}
	}
}
