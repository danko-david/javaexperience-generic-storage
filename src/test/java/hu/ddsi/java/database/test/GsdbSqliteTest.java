package hu.ddsi.java.database.test;

import java.io.File;

import eu.javaexperience.database.ConnectionBuilder;
import eu.javaexperience.database.ConnectionCreator;
import eu.javaexperience.database.JdbcConnectionPool;

public class GsdbSqliteTest extends GsdbTest
{
	protected static String deleteOnExit(String file)
	{
		new File(file).deleteOnExit();
		return file;
	}
	
	public GsdbSqliteTest()
	{
		super
		(
			new JdbcConnectionPool
			(
				ConnectionCreator.fromConnectionBuilder
				(
					ConnectionBuilder.sqlite, 
					"", -1, "", "",
					deleteOnExit("/tmp/test."+System.currentTimeMillis()+".sqlite"))
			)
		);
	}
}
