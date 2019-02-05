package hu.ddsi.java.database;

import hu.ddsi.java.database.fieldAnnotations.*;

public enum GenericStoreMode
{
	GenericStoreDont(GenericStoreIgnore.class,null),//Ne tárolja le!
	GeneriStoreNative(GenericStoreNative.class,null),//Nem tudjuk pontosan milyen típus,vizagáljuk le.
	GenericStoreSerialized(GenericStoreSerialized.class,GenericStoreDataType.Serialized),
	;
	final GenericStoreDataType gtt;
	final Class<?> cls;
	private GenericStoreMode(Class<?> ann,GenericStoreDataType gtt)
	{
		this.gtt = gtt;
		cls = ann;
	} 

	public GenericStoreDataType getGenericStoreDataType()
	{
		return gtt;
	}
	
	public Class<?> getAnnotationType()
	{
		return cls;
	}
}