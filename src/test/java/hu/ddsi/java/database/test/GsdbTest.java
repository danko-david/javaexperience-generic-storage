package hu.ddsi.java.database.test;

import hu.ddsi.java.database.F;
import hu.ddsi.java.database.GenericStorage;
import hu.ddsi.java.database.GenericStoreDatabase;
import hu.ddsi.java.database.GenericStoreException;
import hu.ddsi.java.database.L;
import hu.ddsi.java.database.Limit;
import hu.ddsi.java.database.Offset;
import hu.ddsi.java.database.OrderBy;
import hu.ddsi.java.database.JavaSQLImp.SqlStorage;
import hu.ddsi.java.database.test.model.DChainHead;
import hu.ddsi.java.database.test.model.DEvent;
import hu.ddsi.java.database.test.model.DEventHeader;
import hu.ddsi.java.database.test.model.DLink;
import hu.ddsi.java.database.test.model.DSystemSource;
import hu.ddsi.java.database.test.model.DType;
import hu.ddsi.java.database.test.model.DUser;
import hu.ddsi.java.database.test.model.DbStringListEntry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.junit.Test;

import eu.javaexperience.database.ConnectionCreator;
import eu.javaexperience.database.ConnectionPool;
import eu.javaexperience.database.JDBC;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.time.TimeCalc;
import static org.junit.Assert.*;

public abstract class GsdbTest
{
	static
	{
		/*try
		{
			//NativeLinuxSocketImpl.overrideJavaSocketFacilities();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		JdbcFailSafeConnection.LOG.setLogLevel(LogLevel.DEBUG);
		JavaExperienceLoggingFacility.addStdOut();*/
	}
	
	protected static ConnectionCreator createUdsConnectionCreator(String path, String user, String password, String db)
	{
		return new ConnectionCreator()
		{
			@Override
			public Connection get()
			{
				try
				{
					//&socketFactory=<hu.ddsi.java.database.test.TestGsdb.UdsSocketFactory>&<socket>=/tmp/mysql.sock
					return DriverManager.getConnection("jdbc:mysql:///?socketFactory=hu.ddsi.java.database.test.UdsSocketFactory&socket="+path, user, password);
				}
				catch(Exception e)
				{
					Mirror.propagateAnyway(e);
					return null;
				}
			}
		};
	}
	
	protected ConnectionPool pool;
	
	protected GsdbTest(ConnectionPool pool)
	{
		this.pool = pool;
		
	}
	
	protected static final String db = "test";
	
	protected Connection openTestConnection(boolean reset) throws Throwable
	{
		Connection ret = pool.getConnection();
		ret.setCatalog(db);
		if(reset)
		{
			ArrayList<String> tables = new ArrayList<>();
			JDBC.listTables(ret, tables);
			for(String s:tables)
			{
				JDBC.execute(ret, "DROP TABLE `"+s+"`");
			}
		}
		return ret;
	}
	
	protected GenericStoreDatabase openTestDatabase(boolean reset) throws Throwable
	{
		Connection db = openTestConnection(reset);
		return new SqlStorage(db, "test");
	}
	
	//no error on selecting from nonexisting database
	@Test
	public void test_selectFromNonexistingTable() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			ArrayList<DUser> users = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DUser.class, F.eq.not("do", -1), users, gdb);
			assertEquals(0, users.size());
		}
	}
	
	@Test
	public void test_simpleSelect_KeepSession() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DUser usr = new DUser();
			{
				usr.realFullName = "Dankó Dávid";
				usr.notifyEmail = "info@dankodavid.hu";
				GenericStorage.storeObject(usr, gdb);
			}
			
			{
				ArrayList<DUser> users = new ArrayList<>();
				GenericStorage.getAllObjectsByQuery(DUser.class, F.eq.not("do", -1), users, gdb);
				assertEquals(1, users.size());
				DUser db = users.get(0);
				assertEquals("Dankó Dávid", db.realFullName);
				assertEquals("info@dankodavid.hu", db.notifyEmail);
				assertTrue(usr == db);
			}
		}
	}
	
	@Test
	public void test_simpleSelect_DropSession() throws Throwable
	{
		DUser usr = null; 
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			usr = new DUser();
			{
				usr.realFullName = "Dankó Dávid";
				usr.notifyEmail = "info@dankodavid.hu";
				GenericStorage.storeObject(usr, gdb);
			}
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DUser> users = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DUser.class, F.eq.not("do", -1), users, gdb);
			assertEquals(1, users.size());
			DUser db = users.get(0);
			assertEquals("Dankó Dávid", db.realFullName);
			assertEquals("info@dankodavid.hu", db.notifyEmail);
			assertFalse(usr == db);
		}
	}
	
	@Test
	public void test_cyclicSave_keepSession() throws Throwable
	{
		DLink a = new DLink();
		DLink b = new DLink();
		
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			{
				a.prev = a.next = b;
				b.prev = b.next = a;
				GenericStorage.storeObject(a, gdb);
			}
			
			{
				ArrayList<DLink> links = new ArrayList<>();
				GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
				assertEquals(2, links.size());
				DLink x = links.get(0);
				DLink y = links.get(1);
				
				assertTrue(x.prev == y);
				assertTrue(x.next == y);
				
				assertTrue(y.prev == x);
				assertTrue(y.next == x);
				
				assertTrue(a == x || b == x);
				assertTrue(a == y || b == y);
				
				long ia = x.getGenericStoreData().getID();
				long ib = y.getGenericStoreData().getID();
				assertTrue(ia == 1 || ia == 2);
				assertTrue(ib == 1 || ib == 2);
				assertTrue(ia != ib);
			}
		}
	}
	
	@Test
	public void test_cyclicSave_dropSession() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DLink a = new DLink();
			DLink b = new DLink();
			a.prev = a.next = b;
			b.prev = b.next = a;
			GenericStorage.storeObject(a, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DLink> links = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
			assertEquals(2, links.size());
			DLink x = links.get(0);
			DLink y = links.get(1);
			
			assertTrue(x.prev == y);
			assertTrue(x.next == y);
			
			assertTrue(y.prev == x);
			assertTrue(y.next == x);
			
			long ia = x.getGenericStoreData().getID();
			long ib = y.getGenericStoreData().getID();
			assertTrue(ia == 1 || ia == 2);
			assertTrue(ib == 1 || ib == 2);
			assertTrue(ia != ib);
		}
	}
	
	@Test
	public void test_updateToNull() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DLink a = new DLink();
			DLink b = new DLink();
			a.prev = a.next = b;
			b.prev = b.next = a;
			GenericStorage.storeObject(a, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DLink> links = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
			assertEquals(2, links.size());
			DLink x = links.get(0);
			DLink y = links.get(1);
			
			assertEquals(y, x.prev);
			assertEquals(y, x.next);
			
			assertEquals(x, y.prev);
			assertEquals(x, y.next);
			
			long ia = x.getGenericStoreData().getID();
			long ib = y.getGenericStoreData().getID();
			assertTrue(ia == 1 || ia == 2);
			assertTrue(ib == 1 || ib == 2);
			assertTrue(ia != ib);
			
			x.prev = x.next = null;
			y.prev = y.next = null;
			x.dbMarkModified();
			y.dbMarkModified();
			
			GenericStorage.storeObject(x, gdb);
			GenericStorage.storeObject(y, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DLink> links = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
			assertEquals(2, links.size());
			DLink x = links.get(0);
			DLink y = links.get(1);
			
			assertNull(x.prev);
			assertNull(x.next);
			
			assertNull(y.prev);
			assertNull(y.next);
			
			long ia = x.getGenericStoreData().getID();
			long ib = y.getGenericStoreData().getID();
			assertTrue(ia == 1 || ia == 2);
			assertTrue(ib == 1 || ib == 2);
			assertTrue(ia != ib);
			//listOpen(((SqlStorage)gdb).getConnection());
		}
	}
	
	/*protected static void listOpen(Connection _conn)
	{
		try
		{
			JdbcIssuedConnection conn = (JdbcIssuedConnection) _conn;
			JdbcFailSafeConnection fc = (JdbcFailSafeConnection) Mirror.getObjectFieldOrNull(conn, "origin");
			LinkedList<AutoCloseable> ac = (LinkedList<AutoCloseable>) Mirror.getObjectFieldOrNull(fc, "openedz");
			for(AutoCloseable a:ac)
			{
				Object or = a;
						
				if(a instanceof FailSafeStatement)
				{
					or = Mirror.getObjectFieldOrNull(a, "_");
				}
				
				Object sql = Mirror.getObjectFieldOrNull(or, "sql");
				Object closed = Mirror.getObjectFieldOrNull(or, "closed");
				Object open = Mirror.getObjectFieldOrNull(or, "open");
				//if(closed == Boolean.FALSE || open == Boolean.TRUE)
				{
					System.out.println("sql: "+sql+" closed: "+closed+", open: "+open);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}*/
	
	@Test
	public void test_updateToNull_saveDirection() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DLink a = new DLink();
			DLink b = new DLink();
			a.prev = a.next = b;
			b.prev = b.next = a;
			GenericStorage.storeObject(a, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DLink> links = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
			assertEquals(2, links.size());
			DLink x = links.get(0);
			DLink y = links.get(1);
			
			assertEquals(y, x.prev);
			assertEquals(y, x.next);
			
			assertEquals(x, y.prev);
			assertEquals(x, y.next);
			
			long ia = x.getGenericStoreData().getID();
			long ib = y.getGenericStoreData().getID();
			assertTrue(ia == 1 || ia == 2);
			assertTrue(ib == 1 || ib == 2);
			assertTrue(ia != ib);
			
			//break the link from y to x, but we save from x, and therefore
			//y (because it's still referenced by x) will be saved
			//important: we mark x as modified, however it's not modified for real.
			y.prev = y.next = null;
			x.dbMarkModified();
			y.dbMarkModified();
			
			GenericStorage.storeObject(x, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DLink> links = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DLink.class, F.eq.not("do", -1), links, gdb);
			assertEquals(2, links.size());
			DLink x = links.get(0);
			DLink y = links.get(1);
			
			if(null != y.prev && null == x.prev)
			{
				DLink l = x;
				x = y;
				y = l;
			}
			
			assertEquals(y, x.prev);
			assertEquals(y, x.next);
			
			assertNull(y.prev);
			assertNull(y.next);
			
			long ia = x.getGenericStoreData().getID();
			long ib = y.getGenericStoreData().getID();
			assertTrue(ia == 1 || ia == 2);
			assertTrue(ib == 1 || ib == 2);
			assertTrue(ia != ib);
		}
	}
	
	@Test
	public void test_zigzagSave_then_updateToNull_2_keepSession() throws Throwable
	{
		final Date now = new Date();

		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			{
				DEventHeader eh = new DEventHeader();
				eh.from = TimeCalc.startOfTime;
				eh.to = TimeCalc.endOfTime;
				DSystemSource dss = new DSystemSource();
				dss.createdOn = now;
				dss.issuerUnit = "test system";
				dss.sourceEventType = "test";
				eh.source = dss;
				
				DEvent ev = new DEvent();
				ev.type = DType.CREATE;
				ev.at = now;
				ev.owner = eh;
				
				GenericStorage.storeObject(ev, gdb);
			}
			
			{
				ArrayList<DEvent> evs = new ArrayList<>();
				GenericStorage.getAllObjectsByQuery(DEvent.class, F.eq.not("do", -1), evs, gdb);
				assertEquals(1, evs.size());
				DEvent ev = evs.get(0);
				
				assertEquals(DType.CREATE, ev.type);
				assertEquals(now, ev.at);
				assertNotNull(ev.owner);
				assertEquals(TimeCalc.startOfTime, ev.owner.from);
				assertEquals(TimeCalc.endOfTime, ev.owner.to);
				assertTrue(ev.owner.source instanceof DSystemSource);
				DSystemSource dss = (DSystemSource) ev.owner.source;
				
				assertEquals(now, dss.createdOn);
				assertEquals("test system", dss.issuerUnit);
				assertEquals("test", dss.sourceEventType);
				
				//modify
				ev.type = DType.MODIFY;
				ev.at = TimeCalc.startOfTime;
				ev.owner.to = null;
				dss.issuerUnit = null;
				dss.sourceEventType = "test2";
				/*
				 * ev.owner.dbMarkModified();
				 * 	that's a point of failure, this test pass even if you don't call this method, but if you drop the session
				 * (like in the nex test) it will fail because we don't mark as modified.
				 */
				ev.dbMarkModified();
				dss.dbMarkModified();
				
				GenericStorage.storeAll(gdb, ev, dss);
			}
			
			{
				ArrayList<DEvent> evs = new ArrayList<>();
				GenericStorage.getAllObjectsByQuery(DEvent.class, F.eq.not("do", -1), evs, gdb);
				assertEquals(1, evs.size());
				DEvent ev = evs.get(0);
				
				assertEquals(DType.MODIFY, ev.type);
				assertEquals(TimeCalc.startOfTime, ev.at);
				assertNotNull(ev.owner);
				assertEquals(TimeCalc.startOfTime, ev.owner.from);
				assertNull(ev.owner.to);
				assertTrue(ev.owner.source instanceof DSystemSource);
				DSystemSource dss = (DSystemSource) ev.owner.source;
				
				assertEquals(now, dss.createdOn);
				assertNull(dss.issuerUnit);
				assertEquals("test2", dss.sourceEventType);
			}
		}
	}
	
	@Test
	public void test_zigzagSave_then_updateToNull_2_dropSession() throws Throwable
	{
		final Date now = new Date();
		
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DEventHeader eh = new DEventHeader();
			eh.from = TimeCalc.startOfTime;
			eh.to = TimeCalc.endOfTime;
			DSystemSource dss = new DSystemSource();
			dss.createdOn = now;
			dss.issuerUnit = "test system";
			dss.sourceEventType = "test";
			eh.source = dss;
			
			DEvent ev = new DEvent();
			ev.type = DType.CREATE;
			ev.at = now;
			ev.owner = eh;
			
			GenericStorage.storeObject(ev, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DEvent> evs = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DEvent.class, F.eq.not("do", -1), evs, gdb);
			assertEquals(1, evs.size());
			DEvent ev = evs.get(0);
			
			assertEquals(DType.CREATE, ev.type);
			assertEquals(now, ev.at);
			assertNotNull(ev.owner);
			assertEquals(TimeCalc.startOfTime, ev.owner.from);
			assertEquals(TimeCalc.endOfTime, ev.owner.to);
			assertTrue(ev.owner.source instanceof DSystemSource);
			DSystemSource dss = (DSystemSource) ev.owner.source;
			
			assertEquals(now, dss.createdOn);
			assertEquals("test system", dss.issuerUnit);
			assertEquals("test", dss.sourceEventType);
			
			//modify
			ev.type = DType.MODIFY;
			ev.at = TimeCalc.startOfTime;
			ev.owner.to = null;
			dss.issuerUnit = null;
			dss.sourceEventType = "test2";
			ev.owner.dbMarkModified();
			ev.dbMarkModified();
			dss.dbMarkModified();
			
			GenericStorage.storeAll(gdb, ev, dss);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DEvent> evs = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DEvent.class, F.eq.not("do", -1), evs, gdb);
			assertEquals(1, evs.size());
			DEvent ev = evs.get(0);
			
			assertEquals(DType.MODIFY, ev.type);
			assertEquals(TimeCalc.startOfTime, ev.at);
			assertNotNull(ev.owner);
			assertEquals(TimeCalc.startOfTime, ev.owner.from);
			assertNull(ev.owner.to);
			assertTrue(ev.owner.source instanceof DSystemSource);
			DSystemSource dss = (DSystemSource) ev.owner.source;
			
			assertEquals(now, dss.createdOn);
			assertNull(dss.issuerUnit);
			assertEquals("test2", dss.sourceEventType);
		}
	}
	
	protected static void assertCrossChained(DLink x, DLink y)
	{
		assertEquals(y, x.prev);
		assertEquals(y, x.next);
		
		assertEquals(x, y.prev);
		assertEquals(x, y.next);
		
		long ia = x.getGenericStoreData().getID();
		long ib = y.getGenericStoreData().getID();
		assertTrue(ia != ib);
	}
	
	@Test
	public void test_saveRestoreGsArray_keepSession() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DLink a = new DLink();
			DLink b = new DLink();
			{
				a.prev = a.next = b;
				b.prev = b.next = a;
				
				DChainHead head = new DChainHead();
				head.links = new DLink[]{a, b};
				
				GenericStorage.storeObject(head, gdb);
			}
			
			{
				ArrayList<DChainHead> linkHeads = new ArrayList<>();
				GenericStorage.getAllObjectsByQuery(DChainHead.class, F.eq.not("do", -1), linkHeads, gdb);
				assertEquals(1, linkHeads.size());
				DChainHead l = linkHeads.get(0);
				
				assertCrossChained(l.links[0], l.links[1]);
			}
		}
	}
	
	@Test
	public void test_saveRestoreGsArray_dropSession() throws Throwable
	{
		DLink a = new DLink();
		DLink b = new DLink();

		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			a.prev = a.next = b;
			b.prev = b.next = a;
			
			DChainHead head = new DChainHead();
			head.links = new DLink[]{a, b};
			
			GenericStorage.storeObject(head, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			ArrayList<DChainHead> linkHeads = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery(DChainHead.class, F.eq.not("do", -1), linkHeads, gdb);
			assertEquals(1, linkHeads.size());
			DChainHead l = linkHeads.get(0);
			
			assertCrossChained(l.links[0], l.links[1]);
		}
	}
	
	@Test
	public void test_getById1() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DLink a = new DLink();
			DLink b = new DLink();
			
			DUser usr = new DUser();
			{
				usr.realFullName = "Dankó Dávid";
				usr.notifyEmail = "info@dankodavid.hu";
				GenericStorage.storeObject(usr, gdb);
			}
			
			GenericStorage.storeObject(a, gdb);
			GenericStorage.storeObject(b, gdb);
			
			
			DEventHeader eh = new DEventHeader();
			eh.from = TimeCalc.startOfTime;
			eh.to = TimeCalc.endOfTime;
			DSystemSource dss = new DSystemSource();
			dss.issuerUnit = "test system";
			dss.sourceEventType = "test";
			eh.source = dss;
			
			DEvent ev = new DEvent();
			ev.type = DType.CREATE;
			ev.owner = eh;
			
			GenericStorage.storeObject(ev, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			assertTrue(GenericStorage.getObjectByID(1, gdb) instanceof DUser);
			assertTrue(GenericStorage.getObjectByID(2, gdb) instanceof DLink);
			assertTrue(GenericStorage.getObjectByID(3, gdb) instanceof DLink);
			
			HashSet<Class> clss = new HashSet<Class>();
			clss.add(DEventHeader.class);
			clss.add(DSystemSource.class);
			clss.add(DEvent.class);
			
			assertTrue(isAnyOfThenRemove(GenericStorage.getObjectByID(4, gdb), clss));
			assertTrue(isAnyOfThenRemove(GenericStorage.getObjectByID(5, gdb), clss));
			assertTrue(isAnyOfThenRemove(GenericStorage.getObjectByID(6, gdb), clss));
		}
	}
	
	public static boolean isAnyOfThenRemove(Object o, Collection<Class> clss)
	{
		if(clss.contains(o.getClass()))
		{
			clss.remove(o.getClass());
			return true;
		}
		return false;
	}
	
	@Test
	public void test_orderLimitOffset() throws Throwable
	{
		//pagination
		
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			ArrayList<DbStringListEntry> ents = new ArrayList<>(); 
			for(int i=0;i<1000;++i)
			{
				DbStringListEntry ent = new DbStringListEntry();
				ent.order = i;
				ent.entry = String.valueOf(i);
				ents.add(ent);
			}
			
			GenericStorage.storeAll(ents, gdb);
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			for(int o = 0;o<100;++o)
			{
				ArrayList<DbStringListEntry> ents = new ArrayList<DbStringListEntry>();
				GenericStorage.getObjectsByQuery(gdb, ents, DbStringListEntry.class, F.gt.is("do", "-1"), OrderBy.asc("order"), Offset.offset(10*o), Limit.limit(10));
				for(int i=0;i<10;++i)
				{
					assertEquals(ents.get(i).order, 10*o+i);
				}
			}
		}
		
		try(GenericStoreDatabase gdb = openTestDatabase(false))
		{
			for(int o = 0;o<100;++o)
			{
				ArrayList<DbStringListEntry> ents = new ArrayList<DbStringListEntry>();
				GenericStorage.getObjectsByQuery(gdb, ents, DbStringListEntry.class, F.gt.is("do", "-1"), OrderBy.desc("order"), Offset.offset(10*o), Limit.limit(10));
				for(int i=0;i<10;++i)
				{
					assertEquals(ents.get(i).order, 999-(10*o+i));
				}
			}
		}
	}
	
	protected static void testStringSaveSearch(GenericStoreDatabase gdb, int order, String content) throws GenericStoreException
	{
		DbStringListEntry ent = new DbStringListEntry();
		ent.order = order;
		ent.entry = content;
		GenericStorage.post(gdb, ent);
		
		{
			ArrayList<DbStringListEntry> ret = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery
			(
				DbStringListEntry.class,
				L.and(F.eq.is("order", order), F.eq.is("entry", content)),
				ret,
				gdb
			);
			assertEquals(1, ret.size());
			
			DbStringListEntry e = ret.get(0);
			assertEquals(order, e.order);
			assertEquals(content, e.entry);
		}
		
		{
			ArrayList<DbStringListEntry> ret = new ArrayList<>();
			GenericStorage.getAllObjectsByQuery
			(
				DbStringListEntry.class,
				L.and(F.eq.is("order", order), F.contains.is("entry", content)),
				ret,
				gdb
			);
			
			assertEquals(1, ret.size());
			
			DbStringListEntry e = ret.get(0);
			assertEquals(order, e.order);
			assertEquals(content, e.entry);
		}
	}
	
	@Test
	public void test_str_escape() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			testStringSaveSearch(gdb, 0, "John's workshop");
			testStringSaveSearch(gdb, 1, "John\"s workshop");
			testStringSaveSearch(gdb, 2, "\"Doomsday Machine\"");
			testStringSaveSearch(gdb, 3, "\'Doomsday Machine\'");
			testStringSaveSearch(gdb, 4, "Dr Wu's \"Doomsday Machine\"");
			testStringSaveSearch(gdb, 5, "\'Dr Wu's \"Doomsday Machine\"\"");
			testStringSaveSearch(gdb, 6, "\\'\"Dr Wu\\'\\\"s \"Doomsday Machine\"\"");
			testStringSaveSearch(gdb, 7, "\\\''\\\"\\\'\\\"");
		}
	}
	
	@Test
	public void test_remove() throws Throwable
	{
		try(GenericStoreDatabase gdb = openTestDatabase(true))
		{
			DbStringListEntry e1 = new DbStringListEntry();
			e1.entry = "Str1";
			
			DbStringListEntry e2 = new DbStringListEntry();
			e2.entry = "Str1";
			
			GenericStorage.storeObject(e1, gdb);
			GenericStorage.storeObject(e2, gdb);
			
			
			assertTrue(gdb == GenericStorage.getOwnerDatabase(e1));
			assertTrue(gdb == GenericStorage.getOwnerDatabase(e2));
			
			GenericStorage.removeObject(e1, gdb);
			
			assertTrue(null == GenericStorage.getOwnerDatabase(e1));
			assertTrue(gdb == GenericStorage.getOwnerDatabase(e2));
		}
	}
	
	//TODO getOwnerDatabase should return null after object removed
	
	//TODO offset, limit, orderBy
	
	
	//TODO before save and after restored.
	
	//overwrite model on update (nope, we can't track th referenced elements)
	
	//TODO search with LIKE %asd%
	
	//TODO search for mysql injection
	
	/*@Test
	public void test_saveRestoreArray()
	{
		
	}
	
	
	public void test_mergeNewData_keepSession()
	{
		
	}
	
	public void test_zigzagSave()
	{
		
		
		
	}*/
	
/*********************************** TODO DEV *********************************/
	
	//TODO commit all modification
	//TODO transaction mode
	
	//test not loading lazy load references
	
	//get not loaded lazy references
	
	//check index created
	
	//check compaund index created
	
	//`do` id field index only once
	
	//check table created with alternative name (specified by annotation)
}
