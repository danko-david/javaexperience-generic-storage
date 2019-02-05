package hu.ddsi.java.database.JavaSQLImp;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.javaexperience.database.JDBC;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.Mirror;
import hu.ddsi.java.database.FieldData;
import hu.ddsi.java.database.GenericStorable;
import hu.ddsi.java.database.GenericStorage;
import hu.ddsi.java.database.GenericStoreData;
import hu.ddsi.java.database.GenericStoreData.GenericStorageObjectState;
import hu.ddsi.java.database.GenericStoreDataReader;
import hu.ddsi.java.database.GenericStoreDataType;
import hu.ddsi.java.database.GenericStoreDataWriter;
import hu.ddsi.java.database.GenericStoreDatabase;
import hu.ddsi.java.database.GenericStoreException;
import hu.ddsi.java.database.GenericStoreQueryBuilder.AtomicCondition;
import hu.ddsi.java.database.GenericStoreQueryBuilder.LogicalGroup;
import hu.ddsi.java.database.GenericStoreQueryBuilder.LogicalRelation;
import hu.ddsi.java.database.GenericStoreQueryResult;
import hu.ddsi.java.database.GenericStoreQueryResult.ResultUnit;
import hu.ddsi.java.database.JavaSQLImp.dialects.MysqlDialect;

public class SqlStorage extends GenericStoreDatabase implements Closeable//<ResultSet,SQLCuccWriter>
{
	final Connection connection;
	final String quote;
	final String strQuote;
	private final String idSelect;
	private final String idUpdate0;
	private final String idUpdate1;
	
	public static final String baseDBName = GenericStorable.class.getName();
	
	private String dbName;
	
	protected final SqlDialect dialect;
	
	public SqlStorage(Connection conn, String database) throws SQLException, GenericStoreException
	{
		connection = conn;
		connection.setCatalog(dbName = database);
		dialect = WellKnownSqlDialects.recognise(conn).getDialectManager();
		quote = dialect.getFieldQuoteString();
		strQuote = dialect.getStringQuote();
		idSelect = "SELECT * FROM "+quote+baseDBName+quote+" WHERE "+quote+"do"+quote+"=0;";
		idUpdate0 = "UPDATE "+quote+baseDBName+quote+" SET "+quote+"curId"+quote+"=";
		idUpdate1 = " WHERE "+quote+"do"+quote+"=0;";
			
		init();
		
		mustCallAfterConnectionEstablishedBeforeUse();
	}

	private static final GenericStoreDataWriter writer = new SqlStorageWriter(); 
	
	@Override
	public GenericStoreDataWriter getWriter(String cls) throws Exception
	{
		return writer;
	}
	
	@Override
	public GenericStoreQueryResult getIDListByQuery(Class<? extends GenericStorable> cls, LogicalGroup lg,boolean all_field) throws Exception
	{
		Class crnt = cls;
		
		StringBuilder sb = new StringBuilder();
		if(all_field)
			sb.append("SELECT * FROM ");
		else
			sb.append("SELECT do FROM ");
		sb.append(quote);
		sb.append(crnt.getName());
		sb.append(quote);
		sb.append(" WHERE ");
		buildQuery(sb,lg);
		sb.append(" ;");
		
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(sb.toString());
		long[] ret = Mirror.emptyNLongArray; 
		return new GenericStoreQueryResult(ret, new ResultUnit(cls.getName(), new AutoCloseOnFinalizeRS(st, rs)));
	}
	
	// http://www.w3schools.com/sql/sql_datatypes_general.asp //általános adattípusok
	/**
	 * boolean			=>	BOOLEAN
	 * byte				=>	BINARY(1)
	 * char				=>	CHARACTER(1)
	 * short			=>	SMALLINT
	 * int				=>	INTEGER
	 * float			=>	FLOAT
	 * long				=>	BIGINT
	 * double			=>	DOUBLE PRECISION
	 * 
	 * Date				=> long	=>	BIGINT
	 * String			=>	VARCHAR(...?)
	 * SerialObject		=>	VARBINARY(...?)
	 * Storeid(long)	=> long	=> BIGINT	
	 * */
	@Override
	public void createStorageForClass(Class<? extends GenericStorable> cls, FieldData[] data) throws Exception
	{
		String[] tabs = getTables(connection);
		String clsn = cls.getName();
		
		for(String t:tabs)
		{
			if(clsn.equalsIgnoreCase(t))
				return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		sb.append(quote);
		sb.append(clsn);
		sb.append(quote);
		sb.append(" (");
		
		sb.append(quote);
		sb.append("do");
		sb.append(quote);
		sb.append(" ");
		sb.append(dialect.getCreatePrimitiveKey(Long.class, true, true, true, true));
		
		for(FieldData fd:data)
		{
			if(GenericStoreDataType.DontStore == fd.type)
			{
				continue;
			}
			
			sb.append(",");
			
			sb.append(quote);
			sb.append(fd.getField().getName());
			sb.append(quote);
			
			sb.append(dialect.getSqlType(fd));
		}
		
		sb.append(")");
		
		String opts = dialect.getOtherTableCreateOptions();
		if(null != opts)
		{
			sb.append(opts);
		}
		try
		(
				Statement st = connection.createStatement();
		)
		{
			st.execute(sb.toString());
		}
	}
	
	@Override
	public void dropClassStorageImpl(Class<? extends GenericStorable> cls) throws Exception
	{
		++modificationCount;
		try(Statement st = connection.createStatement())
		{
			st.execute("DROP TABLE "+quote+cls.getName()+quote);
		}
	}
	
	@Override
	public void deleteObjectByIDSByClass(long[] id, Class<? extends GenericStorable>[] cls) throws Exception
	{
		++modificationCount;
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		
		for(int i=0;i<id.length;++i)
		{
			if(i > 0)
				sb.append(",");
			sb.append(id[i]);
			
			cache.remove(id[i]);
		}
		
		sb.append(")");
		
		for(int i=0;i<id.length;++i)
		{
			cache.remove(id[i]);
		}
		
		try(Statement st = connection.createStatement())
		{
			for(Class<? extends GenericStorable> c:cls)
			{
				st.execute("DELETE FROM "+quote+c.getName()+quote+" WHERE do IN"+sb);
			}
		}
	}

	@Override
	public String[] listStoredClasses() throws Exception
	{
		ArrayList<String> ret = new ArrayList<>();
		DatabaseMetaData md = connection.getMetaData();
		try(ResultSet rs = md.getTables(null, null, "%", null))
		{
			while (rs.next())
			{
				ret.add(rs.getString(3));
			}
		}
		return ret.toArray(Mirror.emptyStringArray);
	}

	
	private static final GenericStoreDataReader<AutoCloseOnFinalizeRS> reader = new SqlStorageReader(); 
	
	@Override
	public GenericStoreDataReader getReader(String cls) throws Exception
	{
		return reader;
	}

	/*@Override
	protected AutoCloseOnFinalizeRS findObjectByIdAndClass(long id, String cls) throws SQLException
	{
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery("SELECT * FROM "+quote+cls+quote+" WHERE do="+id);
		return new AutoCloseOnFinalizeRS(st, rs);
	}*/

	@Override
	protected void storeAll(Map<Class<? extends GenericStorable>, List<? extends GenericStorable>> map) throws Exception
	{
		for(Entry<Class<? extends GenericStorable>, List<? extends GenericStorable>> kv:map.entrySet())
		{
			Class<? extends GenericStorable> cls = kv.getKey();
			List<? extends GenericStorable> objects = kv.getValue();
			if(objects.size() > 0)
			{
				GenericStorable inst = objects.get(0);
				GenericStoreDataWriter writer = getWriter(cls.getName());
				FieldData[] fds = getOrCreateFieldData(cls);
				
				writer.writeObjects(objects, this, fds);
			}
		}
	}
	
	@Override
	protected List<Long> reserveNextIDRangeAtomic(int size) throws Exception//TODO atomi növelés és visszaolvasás => do++
	{
		ArrayList<Long> ret = new ArrayList<>();
		if(size > 0)
		{
			++modificationCount;
			try(Statement st = connection.createStatement())
			{
				try(ResultSet rs = st.executeQuery(idSelect))
				{
					rs.next();
					long first = rs.getLong("curId")+1;
					long end = first+size;
					st.execute(idUpdate0+(end-1)+idUpdate1);
					
					for(long l = first;l<end;++l)
					{
						ret.add(l);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public String getDatabaseName()
	{
		return dbName;
	}

	@Override
	public void close()
	{
		try
		{
			connection.close();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getCurrentId() throws Exception
	{
		try(Statement st = connection.createStatement())
		{
			try(ResultSet rs = st.executeQuery(idSelect))
			{
				rs.next();
				return rs.getLong("curId");
			}
		}
	}
	
	static class AutoCloseOnFinalizeRS implements Closeable
	{
		public ResultSet rs;
		public Statement st;
	
		public AutoCloseOnFinalizeRS(Statement st,ResultSet rs)
		{
			this.st = st;
			this.rs = rs;
		}
		
		protected void finalize() throws Throwable
		{
			close();
		}

		@Override
		public void close() throws IOException
		{
			try
			{
				rs.close();
			}
			catch (SQLException e)
			{}
			
			try
			{
				st.close();
			}
			catch (SQLException e)
			{}
		}
	}
	
	public void checkAndAddColumns() throws Exception
	{
		for(String cls:listStoredClasses())
		{
			try
			{
				Class<? extends GenericStorable> c = forName(cls);
				if(null == c || c.isInterface() || Modifier.isAbstract(c.getModifiers()))
				{
					continue;
				}
				
				FieldData[] fds = getOrCreateFieldData(c);
				ArrayList<FieldData> toAdd = new ArrayList<>();
				ArrayList<String> dbf = new ArrayList<>();
				dialect.getTableFields(connection, dbf, cls);
				
				kint:for(FieldData fd:fds)
				{
					for(String f:dbf)
					{
						if(f.equals("do"))
						{
							continue;
						}
						
						if(fd.getField().getName().equals(f))
						{
							continue kint;
						}
					}
					toAdd.add(fd);
				}
				
				if(toAdd.size() > 0)
				{
					for(FieldData f:toAdd)
					{
						try(Statement st = connection.createStatement())
						{
							st.execute
							(
								"ALTER TABLE "
									+quote+cls+quote+
								" ADD "
									+quote+f.getField().getName()+quote+
								" "
									+dialect.getSqlType(f)
							);
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static String[] getTables(Connection conn) throws SQLException
	{
		ArrayList<String> arr = new ArrayList<>();
		DatabaseMetaData md = conn.getMetaData();
	    try(ResultSet rs = md.getTables(null, null, "%", null))
	    {
		    
			while(rs.next())
			{
				arr.add(rs.getString(3));
			}
	    }
		return arr.toArray(Mirror.emptyStringArray);
	  }
	
	public static String[] getDatabases(Connection conn) throws SQLException
	{
		ArrayList<String> arr = new ArrayList<>();
		try(ResultSet rs = conn.getMetaData().getCatalogs())
		{
			while(rs.next())
			{
				arr.add(rs.getString(1));
			}
		}
		return arr.toArray(Mirror.emptyStringArray);
	}
	
	private void init() throws SQLException
	{
		try
		{
			checkAndAddColumns();
		}
		catch(Exception e)
		{
			Mirror.propagateAnyway(e);
		}
		
		String[] dbs = getTables(connection);
		for(String d:dbs)
		{
			if(baseDBName.equalsIgnoreCase(d))
			{
				return;
			}
		}
		
		try
		(
				Statement st = connection.createStatement();
		)
		{
			st.execute("CREATE TABLE "+quote+baseDBName+quote+" ("+quote+"do"+quote+" "+dialect.getCreatePrimitiveKey(Integer.class, true, true, true, true)+","+quote+"curId"+quote+" "+dialect.getCreatePrimitiveKey(Long.class, true, false, false, false)+");");
			st.execute("INSERT INTO "+quote+baseDBName+quote+" VALUES (0,0);");
		}
	}
	
	protected static GenericStorageObjectState getState(GenericStorable gs)
	{
		GenericStoreData data = gs.getGenericStoreData();
		if(null == data)
		{
			return GenericStorageObjectState.NEW;
		}
		
		return data.getState();
	}
	
	public Connection getConnection()
	{
		return connection;
	}
	
	/**
	 * recursive i like you:D
	 * */
	private void buildQuery(StringBuilder sb,LogicalGroup lg)
	{
		switch (lg.getLogicalRelation())
		{
		case and:
		case or:
			boolean nfirst = false;
			for(LogicalGroup g:lg.getLogicalGroups())
			{
				if(nfirst)
					sb.append(lg.getLogicalRelation() == LogicalRelation.and?" AND ":" OR ");
				sb.append("(");
				buildQuery(sb, g);
				sb.append(")");
				nfirst = true;
			}
			break;
			
		case unit:
			AtomicCondition c = lg.getAtomicCondition();
			sb.append(quote);
			sb.append(c.getFieldName());
			sb.append(quote);
			
			switch (c.getOperator())
			{
			case contains:
				if(c.isNegated())
					sb.append(" NOT");
					
				sb.append(" LIKE ");
				sb.append(strQuote);
				sb.append("%");
				
				//mysql fix... see "Note" https://dev.mysql.com/doc/refman/8.0/en/string-comparison-functions.html "To search for \, specify it as \\\\; "
				String add = (String) c.getValue();
				if(null == add)
				{
					add = "null";
				}
				if(dialect instanceof MysqlDialect)
				{
					add = add.replace("\\", "\\\\");
				}
				
				add = toQueryString(add);
				sb.append(add);
				sb.append("%");
				sb.append(strQuote);
				break;
				
			case eq:
				if(c.getValue() == null)
				{
					if(c.isNegated())
						sb.append(" IS NOT NULL");
					else
						sb.append(" IS NULL");
				}
				else
				{
					sb.append(" ");
					if(c.isNegated())
						sb.append("!");
					
					sb.append("= ");
					if(c.getValue() instanceof String)
					{
						sb.append(strQuote);
						sb.append(toQueryString(c));
						sb.append(strQuote);
					}
					else
						sb.append(toQueryString(c));
				}
				break;
				
			case match:
				if(c.isNegated())
					sb.append(" NOT");
					
				sb.append(" REGEX ");
				sb.append(toQueryString(c));
				break;
				
			case gt:
				if(c.isNegated())
					sb.append(" < ");
				else
					sb.append(" >= ");
				
				sb.append(toQueryString(c));
				break;
				
			case gte:
				if(c.isNegated())
					sb.append(" <= ");
				else
					sb.append(" > ");
				
				sb.append(toQueryString(c));
				break;
				
			case lt:
				if(c.isNegated())
					sb.append(" > ");
				else
					sb.append(" <= ");
				
				sb.append(toQueryString(c));
				break;
				
			case lte:
				if(c.isNegated())
					sb.append(" >= ");
				else
					sb.append(" < ");
				
				sb.append(toQueryString(c));
				break;
			
			case in:
				try
				{
					Object val = c.getValue();
					Iterable it = null;
					
					int length = -1;
					if(val instanceof Collection)
					{
						it = (Iterable) val;
						length = ((Collection) val).size();
					}
					else if(val.getClass().isArray())
					{
						ArrayList ar = new ArrayList<>();
						length = Array.getLength(val);
						for(int i=0;i<length;++i)
						{
							ar.add(Array.get(val, i));
						}
						
						it = ar;
					}
					
					if(0 == length)
					{
						if(c.isNegated())
							sb.append(" IS NOT NULL OR TRUE ");
						else
							sb.append(" IS NULL AND FALSE ");
					}
					else
					{
						if(c.isNegated())
							sb.append(" NOT IN ");
						else
							sb.append(" IN ");
						
						JDBC.listing(sb, it, formatValue);
					}
					
					
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				break;
			}
			break;
		}
	}
	
	protected final GetBy1<String, Object> formatValue = new GetBy1<String, Object>()
	{
		@Override
		public String getBy(Object a)
		{
			if(null == a)
			{
				return "null";
			}
			
			if(a instanceof Date)
			{
				return String.valueOf(((Date)a).getTime());
			}
			
			if(a instanceof GenericStorable)
			{
				return String.valueOf(GenericStorage.getID((GenericStorable) a));
			}
			
			return quote(a.toString());
		}
	};
	
	protected String toQueryString(AtomicCondition ac)
	{
		return toQueryString(ac.getValue());
	}
	
	protected String toQueryString(Object o)
	{
		if(o instanceof GenericStorable)
		{
			return String.valueOf(GenericStorage.getID((GenericStorable)o));
		}
		if(o instanceof Date)
		{
			return String.valueOf(((Date)o).getTime());
		}
		if(null != o && o.getClass().isEnum())
		{
			return String.valueOf(((Enum)o).ordinal());
		}
		
		return quote(o.toString());
	}
	
	protected String quote(String str)
	{
		return dialect.escapeString(str);
	}
	
	public static Class forName(String cls)
	{
		try
		{
			//return Class.forName(Strings.getSubstringAfterLastString(cls, "."));
			return Class.forName(cls);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}