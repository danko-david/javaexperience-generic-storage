package hu.ddsi.java.database.test;

import eu.javaexperience.database.ConnectionBuilder;
import eu.javaexperience.database.ConnectionCreator;
import eu.javaexperience.database.JdbcConnectionPool;

public class GsdbMysqlTest extends GsdbTest
{
	protected static final String user = "user";
	protected static final String password = "password";
	
	public GsdbMysqlTest()
	{
		super
		(
			new JdbcConnectionPool
			(
				ConnectionCreator.fromConnectionBuilder
				(
					ConnectionBuilder.mysql,
					"127.0.0.1",
					3306,
					user,
					password,
					db
				)
			)
		);
	}
}
