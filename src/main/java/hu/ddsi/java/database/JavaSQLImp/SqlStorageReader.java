package hu.ddsi.java.database.JavaSQLImp;

import java.sql.SQLException;
import java.util.Date;

import eu.javaexperience.io.SerializationTools;
import eu.javaexperience.text.Format;
import hu.ddsi.java.database.GenericStoreDataArray;
import hu.ddsi.java.database.GenericStoreDataReader;
import hu.ddsi.java.database.GenericStoreDataType;
import hu.ddsi.java.database.JavaSQLImp.SqlStorage.AutoCloseOnFinalizeRS;

public class SqlStorageReader extends GenericStoreDataReader<AutoCloseOnFinalizeRS>
{
	@Override
	protected Object readField(String name, GenericStoreDataType type,AutoCloseOnFinalizeRS rs) throws Exception
	{
		try
		{
			switch(type)
			{
			case Array:
			case Serialized:
			{
				String val = rs.rs.getString(name);
				if(val == null)
					return null;
				return SerializationTools.deserializeFromBlob(Format.fromHex(val));
			}
			case Boolean:
				return rs.rs.getBoolean(name);
				
			case Byte:
				return rs.rs.getBytes(name)[0];
				
			case Char:
				return rs.rs.getString(name).charAt(0);
	
			case Date:
				return new Date(rs.rs.getLong(name));
	
			case DontStore:
				return null;
				
			case Double:
				return rs.rs.getDouble(name);
			
			case Float:
				return rs.rs.getFloat(name);
				
			case Enum:
			case Integer:
				return rs.rs.getInt(name);
					
			case GenericDataId:
				long val = rs.rs.getLong(name);
				if(val == 0)
					return null;
				return val;
				
			case Long:
				return rs.rs.getLong(name);
				
			case Short:
				return rs.rs.getShort(name);
	
			case String:
				return rs.rs.getString(name);
			}
		}
		catch(Exception e)
		{
			//TODO debug
			e.printStackTrace();
			throw e;
		}
		finally
		{
			if(rs.rs.wasNull())//hehe
			{
				return null;
			}
		}
		
		return null;
	}

	@Override
	protected Object readSerializedField(String name, AutoCloseOnFinalizeRS rs) throws Exception
	{
		String data = rs.rs.getString(name);
		if(null != data)
		{
			try
			{
				return SerializationTools.deserializeFromBlob(stringToBlob(data));
			}
			catch(Exception e)
			{
				return SerializationTools.deserializeFromBlob(Format.fromHex(rs.rs.getString(name)));
			}
		}
		return null;
	}
	
	public static final byte[] stringToBlob(String adat)
	{
		if(adat.length()%2!=0) throw new IllegalArgumentException("A String nem tartlmaz blob adatot!");

		byte[] ki = new byte[adat.length()/2];
		int pointer = 0;
		int strlen = adat.length();
		byte buf = 0;
		for(int i=0;i<strlen;i+=2)
		{
			buf =(byte) (hexVal(adat.charAt(i)));
			buf <<= 4;
			buf |= (byte)(hexVal(adat.charAt(i+1)));
			ki[pointer] = (byte) (buf-128);
			pointer++;
		}

		return ki;
	}
	
	private static final int hexVal(char c)
	{
		switch(c)
		{
			case '0':
				return 0;
			case '1':
				return 1;
			case '2':
				return 2;
			case '3':
				return 3;
			case '4':
				return 4;
			case '5':
				return 5;
			case '6':
				return 6;
			case '7':
				return 7;
			case '8':
				return 8;
			case '9':
				return 9;
			case 'a':
				return 10;
			case 'b':
				return 11;
			case 'c':
				return 12;
			case 'd':
				return 13;
			case 'e':
				return 14;
			case 'f':
				return 15;
			case 'A':
				return 10;
			case 'B':
				return 11;
			case 'C':
				return 12;
			case 'D':
				return 13;
			case 'E':
				return 14;
			case 'F':
				return 15;
			default:
				return -1;
		}

	}

	@Override
	protected Object[] readArray(String name, GenericStoreDataArray arr, AutoCloseOnFinalizeRS rs)throws Exception
	{
		return (Object[]) readSerializedField(name, rs);
		//return (Object[]) DDSI.byteArrayToObject(DDSI.stringToBlob(rs.getBytes(name)));
	}

	@Override
	protected String getClassBy(AutoCloseOnFinalizeRS cur) throws SQLException
	{
		return cur.rs.getMetaData().getTableName(1);
	}

	@Override
	protected long getIdBy(AutoCloseOnFinalizeRS cur) throws SQLException
	{
		return cur.rs.getLong("do");
	}

	@Override
	protected boolean nextResult(AutoCloseOnFinalizeRS cur) throws SQLException
	{
		return cur.rs.next();
	}

	@Override
	protected boolean setReadyToRead(AutoCloseOnFinalizeRS cur) throws SQLException
	{
		return cur.rs.next();
	}
	
}