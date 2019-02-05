package hu.ddsi.java.database;

import java.util.Map;

import hu.ddsi.java.database.GenericStorable;
import hu.ddsi.java.database.GenericStoreData;
import hu.ddsi.java.database.GenericStoreDatabase;
import hu.ddsi.java.database.GenericStoreMode;
import hu.ddsi.java.database.GenericStoreData.GenericStorageObjectState;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;

public class GsdbModel implements GenericStorable
{
	@GenericStoreIgnore
	private transient GenericStoreData OiCLD6cRWgYPaG3Cm7Ty;
	
	@Override
	public GenericStoreData getGenericStoreData()
	{
		return OiCLD6cRWgYPaG3Cm7Ty;
	}

	@Override
	public void setGenericStoreData(GenericStoreData data)
	{
		OiCLD6cRWgYPaG3Cm7Ty = data;
	}

	@Override
	public Map<String, GenericStoreMode> getSelfDefinedMapping()
	{
		return null;
	}

	@Override public void beforeStored(GenericStoreDatabase db){}

	@Override public void afterRestored(GenericStoreDatabase db){}
	
	public void dbMarkModified()
	{
		if(null != OiCLD6cRWgYPaG3Cm7Ty)
		{
			OiCLD6cRWgYPaG3Cm7Ty.state = GenericStorageObjectState.MODIFIED;
		}
	}
}
