package hu.ddsi.java.database;

import hu.ddsi.java.database.fieldAnnotations.GenericStoreIgnore;
import hu.ddsi.java.database.fieldAnnotations.GenericStoreStorableID;

import java.util.Map;

/**
 * <div class="hu">
 * Annak az osztálynak amely megvalósítja ezt az interface-t, rendelkeznie kell ÜRES KONSTRUKTORRAL!
 * Különben lekéréskor mikor úgy példány készülne az kivétel dobásával fog végződni.
 * Miért nem absztrakt osztály? Mert "megvonja" az öröklést, így az osztály örökölhet is és lehet tárolható is.
 * </div>
 * <div class="en">
 * Classes what implements this interface can be stored through {@link GenericStorage} mechanism.
 * To this class can be retrieved, the class shall have EMPTY CONSTRUCTOR!
 * else getting class from database will throw exception.
 * Why not implement class data storage with abstract class?
 * Because class can't inherite from abstract class and other class in same time.  
 * </div>
 * */
public interface GenericStorable
{
	/**
	 * <div class="hu">
	 * Minden példánynak rendelkeznie kell egy {@link GenericStoreData} ami kezeli a szinkronizációt, tárolja az id-jét és hogy módosítottuk-e.  
	 * Hogy az objektumon belül milyen mezőnévvel látjuk el az belső kérdés de azt a mezőt lássuk el {@link GenericStoreIgnore} annotációval.
	 * </div>
	 * </div class="en">
	 * All instance shall have a unique {@link GenericStoreData} which handle the storage (store the id and so).
	 * Name of the field where we store the reference is never mind, just put the {@link GenericStoreIgnore} annotation on it.
	 * </div>
	 * */
	GenericStoreData getGenericStoreData();
	
	/**
	 * <div class="hu">
	 * Ezzel állíthatjuk be a kezelő objektumot.
	 * </div>
	 * <div class="en">
	 * Set the {@link GenericStoreData} reference.
	 * </div>
	 * */
	void setGenericStoreData(GenericStoreData data);

	/**
	 * <div class="hu">
	 * Leszármazott osztályoknál nem férünk hozzá a szülő kódjához, így mezőiket nem tudunk annotációval ellátani.
	 * Ha ez a metódus egy olyan mapot ad vissza ami a mező neve a tárolási módjához van kötve, akkor az alapértelmezett
	 * helyett az itt meghatározottak szerint fogja tárolni a megadott mezőt.
	 * Ha null-t adsz vissza akkor az alapértelmezett tárolási mód szerint fogja a mezőket tárolni, ami a {@link GenericStorage}-ben van meghatározva.
	 * Azok a mezők amelyekre nincs határozat a null kulcshoz társított módon lesznek tárolva.
	 * Ha az is null akkor a mező nem lesz tárolva.
	 * </div>
	 * <div clas="en">
	 * In inherited classes we can't access the parent field to put the annotations, but all of the object filed can be stored even if it's in parent.
	 * You can define the storage for this classes through a Map which constain "field name" - "storage mode pairs".
	 * If you give null back the fileds will be stored by the default mode which specified in {@link GenericStorage}.
	 * If the field storage mode not specified for a field in the Map, then it will be stored by the Storage mode returned by the null key.
	 * If the value of the null key is null then the field will not stored.  
	 * </div>
	 * */
	Map<String,GenericStoreMode> getSelfDefinedMapping();
	
	/**
	 * <div class="hu">
	 * Mielőtt eltárolnánk az objektumot ez a metódus lefut.
	 * Hasznos hogyha egy mező maga is tárolható az adatbázisban, de te csak az azonosítóját akarod tárolni,
	 * akkor az adott objektum azonosítóját eltárolhatod a tárolandó objektum egy tárolandó mezőjébe.
	 * Miért is jó ez? Ha olyan objektum referenciáját tárolod ami maga is tárolható adatbázisban. DE ha te kikéred az 
	 * adatbázisból és az a mező {@link GenericStoreStorableID} annotációval lett ellátva akkor azt is kiéri az adatbázisból.
	 * Ha van egy nagy előreláncolt listád amit adatbázisban akarsz tárolni és kikéred az egyik elemét, akkor az utána következő összes elem
	 * is bekerül a gépbe. Ilyenkor csak az id-jét tárold el! Hozz létre egy mezőt amit nem tárolsz és abba lusta inicializáláskor csak akkor tedd bele ha valaki get-eli. 
	 * Vagy: az absztrakt adatbázis kezelőbe beépítettem egy gyorsítótárat, így a getterrel közvetlen elkérheted az adatbázistól a tárolt azonosítójú objektumot,
	 * setelésnél pedig frissítheted az azonosítót. Az előbbi gyorsabb, és biztosabb.
	 * </div>
	 * <div class="en">
	 * Before object will be stored this method will be called.
	 * This can be usefull if you have a another object int current object which is also can be stored but you don't want to store them.
	 * If you just need the ID of the object you can read it amd store to a ther field which is in current object, so only the id of it's is stored. 
	 * Why is good for you? If you store a another storable element's referece it will be also stored in the database. If you get this object (which you now want to store, not the referenced)
	 * on the phase when the object readed from database, this referenced object will be also readed and placed into this object filed. What if you store a lognd forward chained list in database?
	 * If you get a object from the list, all of object which followed by this, also will be readed from database. But if this object is also storable in databse, and you don't want to restore always
	 * You can store only the Object's id. Write a getter/setter method for it, and in this method let's store it's reference in a long field.
	 * In this situation the best if you create a transient field for that object, and a long field for it's id, on get, if Object is null, then if id is 0 the return null else get the object from database
	 * and store in that transient field.
	 * Or: Abstract database handler contains a cache, on getting the field you can directly get from database, on set you can store the object and update ID. But previous is a better solution (i think).       
	 * </div>
	 * */
	void beforeStored(GenericStoreDatabase db);
	
	/**
	 * <div class="hu">
	 * Miután az objektum kiolvasásra került az adatbázisból ez a metódus fut le.
	 * </div>
	 * <div class="en">
	 * After object is restored this method will be called. 
	 * </div>
	 * */
	void afterRestored(GenericStoreDatabase db);
}