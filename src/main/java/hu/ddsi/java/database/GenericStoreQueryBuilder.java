package hu.ddsi.java.database;

public class GenericStoreQueryBuilder
{
	public static enum LogicalRelation
	{
		and,
		or,
		unit,
	}
	
	public static class LogicalGroup
	{
		public static LogicalGroup[] emptyLogicalGroupArray = new LogicalGroup[0]; 
		
		private final LogicalRelation lr;
		private final LogicalGroup[] components;
		private final AtomicCondition ac;
		
		public LogicalGroup(AtomicCondition ac)
		{
			this.ac = ac;
			components = null;
			lr = LogicalRelation.unit;
		}

		public LogicalGroup(LogicalRelation lr,LogicalGroup[] grp)
		{
			this.ac = null;
			components = grp;
			this.lr = lr;
		}
		
		public LogicalRelation getLogicalRelation()
		{
			return lr;
		}
		
		public AtomicCondition getAtomicCondition()
		{
			return ac;
		}
		
		public LogicalGroup[] getLogicalGroups()
		{
			return components;
		}
		
		@Override
		public String toString()
		{
			if(null != ac)
			{
				return ac.toString();
			}
			
			StringBuilder sb = new StringBuilder();
			
			for(LogicalGroup c:components)
			{
				if(sb.length() > 0)
				{
					sb.append(" ");
					sb.append(lr.name());
					sb.append(" ");
				}
				sb.append(c.toString());
			}
			
			return sb.toString();
		}
	}
	
	public static interface ConditionInterface
	{
		public LogicalGroup not(String symbol,Object obj);
		public LogicalGroup is(String symbol,Object obj);
	}
	
	public static class AtomicCondition
	{
		private final F operator;
		private final boolean negate;
		private final String field;
		private final Object value;
		
		public AtomicCondition(F operator,boolean negate,String field,Object val)
		{
			this.operator = operator;
			this.negate = negate;
			this.field = field;
			this.value = val;
		}

		public F getOperator()
		{
			return operator;
		}

		public boolean isNegated()
		{
			return negate;
		}

		public String getFieldName()
		{
			return field;
		}

		public Object getValue()
		{
			return value;
		}
		
		@Override
		public String toString()
		{
			return field+" "+(negate?"!":"")+operator.name()+" "+value;
		}
	}
	
	
}