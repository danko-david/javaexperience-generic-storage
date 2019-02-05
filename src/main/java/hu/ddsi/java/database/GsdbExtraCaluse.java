package hu.ddsi.java.database;

public interface GsdbExtraCaluse
{
	public GsdbExtraClauseType getType();
	
	public static enum GsdbExtraClauseType
	{
		order,
		offset,
		limit,
		group
	}
}
