package hu.ddsi.java.database;

public class Limit implements GsdbExtraCaluse
{
	@Override
	public GsdbExtraClauseType getType()
	{
		return GsdbExtraClauseType.limit;
	}
	
	public final int limit;
	
	public Limit(int limit)
	{
		this.limit = limit;
	}
	
	public static Limit limit(int l)
	{
		return new Limit(l);
	}
}
