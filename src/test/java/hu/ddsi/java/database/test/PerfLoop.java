package hu.ddsi.java.database.test;

public class PerfLoop
{
	//TODO integrate performance framework
	public static void main(String[] args) throws Throwable
	{
		/*TestGsdb test = new TestGsdb();
		JdbcFailSafeConnection.LOG.setLogLevel(LogLevel.INFO);
		long dt = 5_000;
		MeasurementSerie serie = new MeasurementSerie();
		long tp = System.currentTimeMillis();
		while(true)
		{
			long t0 = System.currentTimeMillis();
			test.test_selectFromNonexistingTable();
			test.test_simpleSelect_KeepSession();
			test.test_simpleSelect_DropKeepSession();
			test.test_cyclicSave_keepSession();
			test.test_cyclicSave_dropSession();
			test.test_updateToNull();
			test.test_updateToNull_saveDirection();
			test.test_zigzagSave_then_updateToNull_2_keepSession();
			test.test_zigzagSave_then_updateToNull_2_dropSession();
			test.test_saveRestoreGsArray_keepSession();
			test.test_saveRestoreGsArray_dropSession();
			serie.add(System.currentTimeMillis()-t0);
			
			if(System.currentTimeMillis() - tp > dt)
			{
				tp = System.currentTimeMillis();
				
				serie.dropUpperAndLower5Percent();
				System.out.println(serie.getShortStat());
				serie = new MeasurementSerie();
			}
		}*/
	}
}
