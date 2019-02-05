package hu.ddsi.java.database;

public class Offset implements GsdbExtraCaluse
{
	@Override
	public GsdbExtraClauseType getType()
	{
		return GsdbExtraClauseType.offset;
	}
	
	public final int offset;
	
	public Offset(int offset)
	{
		this.offset = offset;
	}
	
	public static Offset offset(int o)
	{
		return new Offset(o);
	}
}
