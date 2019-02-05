package hu.ddsi.java.database.JavaSQLImp;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.database.JDBC;
import eu.javaexperience.interfaces.simple.SimpleGetFactory;
import eu.javaexperience.io.SerializationTools;
import eu.javaexperience.text.Format;
import hu.ddsi.java.database.FieldData;
import hu.ddsi.java.database.GenericStorable;
import hu.ddsi.java.database.GenericStorage;
import hu.ddsi.java.database.GenericStoreDataArray;
import hu.ddsi.java.database.GenericStoreDataType;
import hu.ddsi.java.database.GenericStoreDataWriter;
import hu.ddsi.java.database.GenericStoreDatabase;
import hu.ddsi.java.database.GenericStoreException;
import hu.ddsi.java.database.JavaSQLImp.SqlStorageWriter.SQLCuccWriter;

public class SqlStorageWriter extends GenericStoreDataWriter<SqlStorage,SQLCuccWriter>
{
	public static class SQLCuccWriter
	{
		public SQLCuccWriter(SqlStorage st,String cls,boolean update)
		{
			db = st;
			this.cls = cls;
			//this.update = update;
		}
		
		SqlStorage db;
		String cls;
		StringBuilder vals = null;
		StringBuilder fields = new StringBuilder();
		boolean nfirst = false;
		boolean update;
		
		private void comma()
		{
			if(nfirst)
			{
				if(!update)
					fields.append(",");
				vals.append(",");
			}
			else
				nfirst = true;
		}
	}
	
	public SQLCuccWriter newWriter(SqlStorage db, Class cls, boolean update)
	{
		update = false;
		SQLCuccWriter ret = new SQLCuccWriter(db, cls.getName(), update);
		if(update)
		{
			ret.vals = ret.fields;
			ret.vals.append("UPDATE ");
			ret.vals.append(db.quote);
			ret.vals.append(cls.getName());
			ret.vals.append(db.quote);
			ret.vals.append(" SET ");
		}
		else
		{
			ret.vals = new StringBuilder();
		}
		
		return ret;
	}
	
	@Override
	public void writeField(String name, FieldData type, Object value, SQLCuccWriter w) throws Exception
	{
		w.comma();
		w.fields.append(w.db.quote);
		w.fields.append(name);
		w.fields.append(w.db.quote);
		
		if(w.update)
			w.fields.append("=");
		
		putField(w.vals, w.db, type, value);
	}

	public static void putField(StringBuilder vals, GenericStoreDatabase gdb, FieldData fd, Object value) throws Exception
	{
		if(value == null)
		{
			vals.append("null");
			return;
		}
		
		switch(fd.getDataType())
		{
		case Date:
			vals.append(((Date)value).getTime());
			break;
		
		case Boolean:
			vals.append(Boolean.TRUE.equals(value)?'1':'0');
			
			break;

		case Enum:
			vals.append(((Enum<?>)value).ordinal());
			break;
			
		case Byte:
		case Double:
		case Float:
		case Integer:
		case Long:
		case Short:
			vals.append(value.toString());
			break;

		case GenericDataId:
			GenericStorable gs = (GenericStorable) value;
			if(null != gdb)
			{
				GenericStorage.storeObject(gs, gdb);
			}
			vals.append(GenericStorage.getID(gs));
			break;
			
		case Char:
		case String:
			SqlDialect dial = ((SqlStorage)gdb).dialect;
			String q = dial.getStringQuote();
			vals.append(q);
			vals.append(dial.escapeString((String)value));
			vals.append(q);
			break;
			
		case DontStore:
			return;
			
		case Serialized:
		case Array:
			appendArray(gdb, fd, vals, value);
			break;
		}
	}
	
	protected static void appendArray(GenericStoreDatabase gdb, FieldData type, StringBuilder dst, Object val) throws GenericStoreException
	{
		if(type.getArrayData().getDataType() == GenericStoreDataType.GenericDataId)
		{
			val = conventToLongArray((GenericStorable[])val,gdb);
		}
		dst.append("'");
		dst.append(Format.toHex(SerializationTools.serializeIntoBlob((Serializable) val)));
		dst.append("'");
	}
	
	@Override
	public void writeArrayField(String name, GenericStoreDataArray arr,Object value,SQLCuccWriter w) throws Exception
	{
		w.comma();
		w.fields.append(name);
		if(w.update)
			w.vals.append("=");
		//Ez de hulladék megoldás
		w.vals.append("'"+Format.toHex(SerializationTools.serializeIntoBlob((Serializable) value))+"'");
	}

	@Override
	public void flushObject(SQLCuccWriter w) throws Exception
	{
		if(w.update)
		{
			try(Statement st = w.db.connection.createStatement())
			{
				st.execute(w.vals.toString());
			}
		}	
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("REPLACE INTO ");
			sb.append(w.db.quote);
			sb.append(w.cls);
			sb.append(w.db.quote);
			sb.append("(");
			sb.append(w.fields);
			sb.append(")VALUES(");
			sb.append(w.vals);
			sb.append(");");
			try(Statement st = w.db.connection.createStatement())
			{
				st.execute(sb.toString());
			}
		}
		
	}
	
	@Override
	public void writeID(long id, SQLCuccWriter w) throws Exception
	{
		if(w.update)
			w.vals.append(" WHERE ");
		else
			w.comma();
		w.fields.append(w.db.quote);
		w.fields.append("do");
		w.fields.append(w.db.quote);
		if(w.update)
			w.vals.append("=");
		w.vals.append(id);
	}
	
	protected static void writeStart(StringBuilder sb, String quote, String cls, FieldData[] fds)
	{
		sb.append("REPLACE INTO ");
		sb.append(quote);
		sb.append(cls);
		sb.append(quote);
		sb.append("(`do`");
		for(FieldData fd:fds)
		{
			sb.append(",`");
			sb.append(fd.f.getName());
			sb.append("`");
		}
		sb.append(")VALUES");
	}
	
	protected static void writeObject(StringBuilder sb, String quote, String cls, FieldData[] fds, GenericStoreDatabase gdb, List<? extends GenericStorable> objects) throws Exception
	{
		int n = 0;
		for(GenericStorable obj:objects)
		{
			if(++n > 1)
			{
				sb.append(",");
			}
			
			sb.append("(");
			sb.append(GenericStorage.getID(obj));
			sb.append(",");
			int fn = 0;
			for(FieldData fd:fds)
			{
				if(++fn > 1)
				{
					sb.append(",");
				}
				putField(sb, gdb, fd, fd.f.get(obj));
			}
			sb.append(")");
		}
	}


	@Override
	public void writeObjects(List<? extends GenericStorable> objects, GenericStoreDatabase _gdb, FieldData[] fds) throws Exception
	{
		SqlStorage gdb = (SqlStorage) _gdb;
		int shardSize = 1000;
		
		if(objects.size() > 0)
		{
			
			String quote = gdb.quote;
			Class cls = objects.get(0).getClass();
			gdb.createStorageForClassIfNeeded(cls, fds);
			String clsn = cls.getName();
			StringBuilder sb = new StringBuilder();
			List<List<GenericStorable>> shards = (List)CollectionTools.shard(objects, shardSize, SimpleGetFactory.getArrayListFactory(), SimpleGetFactory.getArrayListFactory());
			for(List<GenericStorable> s:shards)
			{
				sb.delete(0, sb.length());
				if(s.size() > 0)
				{
					writeStart(sb, gdb.quote, clsn, fds);
					writeObject(sb, quote, clsn, fds, gdb, objects);
					JDBC.execute(gdb.connection, sb.toString());
				}
			}
		}
	}
}