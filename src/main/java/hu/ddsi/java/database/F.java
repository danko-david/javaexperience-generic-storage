package hu.ddsi.java.database;

import hu.ddsi.java.database.GenericStoreQueryBuilder.AtomicCondition;
import hu.ddsi.java.database.GenericStoreQueryBuilder.ConditionInterface;
import hu.ddsi.java.database.GenericStoreQueryBuilder.LogicalGroup;

public enum F implements ConditionInterface 
{
	eq,
	lt,
	gt,
	gte,
	lte,
	contains,
	match,
	in,
	;

	@Override
	public LogicalGroup not(String symbol, Object obj)
	{
		return L.u(new AtomicCondition(this, true, symbol, obj));
	}

	@Override
	public LogicalGroup is(String symbol, Object obj)
	{
		return L.u(new AtomicCondition(this, false, symbol, obj));
	}
}

