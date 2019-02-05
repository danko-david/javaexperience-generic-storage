package hu.ddsi.java.database.JavaSQLImp;

public class SQLStorageCreateOptions
{
	private final int stringMaxLength;
	//private final int blobMaxLength;

	public SQLStorageCreateOptions(int stringMaxLength/*,int blobMaxLength*/)
	{
		this.stringMaxLength = stringMaxLength;
		/*this.blobMaxLength = blobMaxLength;*/
	}
	
	public int getStringMaxLength()
	{
		return stringMaxLength;
	}
	
/*	public int getBlobMaxLength()
	{
		return blobMaxLength;
	}*/
}