package hu.ddsi.java.database.JavaSQLImp.dialects;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import eu.javaexperience.database.JDBC;
import eu.javaexperience.exceptions.UnimplementedCaseException;
import eu.javaexperience.text.StringTools;
import hu.ddsi.java.database.FieldData;
import hu.ddsi.java.database.JavaSQLImp.SqlDialect;
import hu.ddsi.java.database.JavaSQLImp.SqlStorage;

public class MysqlDialect implements SqlDialect
{
	@Override
	public boolean probeDialect(Connection conn)
	{
		try
		{
			String versionString = JDBC.getString(conn, "SELECT @@VERSION");
			versionString = versionString.toLowerCase();
			return versionString.contains("mysql") || versionString.contains("mariadb");	
		}
		catch(Exception e)
		{
			return false;
		}
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
			return "VARCHAR("+fd.getMaxSize()+") CHARACTER SET utf8";
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
		return "\nDEFAULT CHARACTER SET = utf8\nCOLLATE = utf8_bin;";
	}

	@Override
	public void getTableFields(Connection connection, Collection<String> dbf, String table) throws SQLException
	{
		try(Statement st = connection.createStatement())
		{
			try(ResultSet rs = st.executeQuery("SHOW COLUMNS FROM "+getFieldQuoteString()+table+getFieldQuoteString()))
			{
				while(rs.next())
				{
					dbf.add(rs.getString(1));
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
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
