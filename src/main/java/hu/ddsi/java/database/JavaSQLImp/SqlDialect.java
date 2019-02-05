package hu.ddsi.java.database.JavaSQLImp;

import hu.ddsi.java.database.FieldData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import eu.javaexperience.semantic.references.MayNull;

public interface SqlDialect
{
	public boolean probeDialect(Connection conn);
	public String getSqlType(FieldData fd);
	public String getFieldQuoteString();
	public String getStringQuote();
	public String getCreatePrimitiveKey(Class type, boolean notNull,boolean unique, boolean primary, boolean indexed);
	public @MayNull String getOtherTableCreateOptions();
	public void getTableFields(Connection connection, Collection<String> dbf, String table) throws SQLException;
	public String escapeString(String value);
	/*public String commandStartTransaction();
	public String commandCommit();*/
}