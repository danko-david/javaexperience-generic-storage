package hu.ddsi.java.database.JavaSQLImp.dialects;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import eu.javaexperience.database.JDBC;
import eu.javaexperience.exceptions.UnimplementedCaseException;
import hu.ddsi.java.database.FieldData;
import hu.ddsi.java.database.JavaSQLImp.SqlDialect;

public class SqliteDialect implements SqlDialect
{
	@Override
	public boolean probeDialect(Connection conn)
	{
		try
		{
			return null != JDBC.getString(conn, "select sqlite_version()");
		}
		catch(Exception e){}
		
		return false;
	}

	@Override
	public String getSqlType(FieldData fd)
	{
		switch (fd.getDataType())
		{
		
		case Serialized:
		case Array:
			return "VARCHAR("+fd.getMaxSize()+")";
			
		case Boolean:
			return "BOOLEAN";
			
		case Byte:
			return "BINARY(1)";
			
		case Char:
			return "CHARACTER(1)";
			
		case Long:
		case Date:
		case GenericDataId:
			return "BIGINT";
			
		case DontStore:
			return null;

		case Double:
			return "DOUBLE PRECISION";
			
		case Integer:
		case Enum:
			return "INTEGER";
			
		case Float:
			return "FLOAT";
			
		case Short:
			return "SMALLINT";
			
		case String:
			return "VARCHAR("+fd.getMaxSize()+")";
		}
		
		return null;
	}

	@Override
	public String getFieldQuoteString()
	{
		return "`";
	}

	@Override
	public String getStringQuote()
	{
		return "\"";
	}

	@Override
	public String getCreatePrimitiveKey(Class type, boolean notNull, boolean unique, boolean primary, boolean indexed)
	{
		StringBuilder sb = new StringBuilder();
		if(Integer.class == type)
		{
			sb.append("INTEGER");
		}
		else if(Long.class == type)
		{
			sb.append("BIGINT");
		}
		else
		{
			throw new UnimplementedCaseException(type.toString());
		}
		
		if(notNull)
		{
			sb.append(" NOT NULL");
		}
		
		if(unique)
		{
			sb.append(" UNIQUE");
		}
		
		if(primary)
		{
			sb.append(" PRIMARY KEY");
		}
		return sb.toString();
	}

	@Override
	public String getOtherTableCreateOptions()
	{
		return "";
	}

	@Override
	public void getTableFields(Connection connection, Collection<String> dbf, String table) throws SQLException
	{
		try(Statement st = connection.createStatement())
		{
			try(ResultSet rs = st.executeQuery("PRAGMA table_info('"+table+"')"))
			{
				while(rs.next())
				{
					dbf.add(rs.getString(2));
				}
			}
		}		
	}

	@Override
	public String escapeString(String value)
	{
		if(null == value)
		{
			return "null";
		}
		return value.toString().replace("\"", "\"\"");
	}
	
	/*@Override
	public String commandStartTransaction()
	{
		return "BEGIN TRANSACTION";
	}

	@Override
	public String commandCommit()
	{
		return "COMMIT TRANSACTION";
	}*/
}
