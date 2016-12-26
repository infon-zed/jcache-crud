package ru.infon.jcache.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import ru.infon.jcache.core.StorableEntry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import java.util.Date;

/**
 * 20.10.2016
 * @author kostapc
 * 2016 Infon
 */

public class MongoCacheEntity<K,V> {

    public static final String FIELD_KEY = "key";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_MONGO_ID = "_id";
    public static final String FIELD_SAVE_DATE = "save_date";
    public static final String FIELD_EXPIRE_TIMESTAMP = "expire_timestamp";


    //private final MongoConnection mongo;
    private MongoEntrySupplier<K,V> supplier;
    private DBObject sourceObject;

    private ObjectId id;

    private final StorableEntry<K,V> entry;

    public MongoCacheEntity(
            StorableEntry<K,V> entry,
            MongoEntrySupplier<K,V> supplier
    ) { // NEW CACHE OBJECT
        this.entry = entry;
        this.supplier = supplier;
        this.sourceObject = createDBObject();
        this.id = null;
    }

    public MongoCacheEntity(
            DBObject sourceObject,
            Class<K> keyType, Class<V> valueType,
            MongoEntrySupplier<K,V> supplier
    ) { // EXISTED CACHE OBJECT
        this.sourceObject = sourceObject;
        //this.mongo = mongo;
        this.supplier = supplier;
        this.id = (ObjectId)sourceObject.get(FIELD_MONGO_ID);
        // -------- create and fill storable entry object
        DBObject valueDBObject = (DBObject) sourceObject.get(FIELD_VALUE);
        K key = keyType.cast(sourceObject.get(FIELD_KEY));
        V value = supplier.parseValueDBObject(valueDBObject);
        entry = supplier.createEntry(key, value);
        entry.setSaveDate(getDateField(FIELD_SAVE_DATE));
        entry.setExpireTimestamp((Long) sourceObject.get(FIELD_EXPIRE_TIMESTAMP));
    }

    private Date getDateField(String field) {
        return (Date) this.sourceObject.get(field);
    }

    DBObject getDBObject() {
        return createDBObject();
    }

    Bson getFindQuery() {
        if(id==null) {
            return MongoQueryHelper.createFindQuery(entry.getKey());
        } else {
            return new BasicDBObject(FIELD_MONGO_ID, id);
        }
    }

    StorableEntry<K,V> getStorableEntiry() {
        return entry;
    }

    // TODO: change it to store/update query; update by _id;
    private DBObject createDBObject() {

        BasicDBObject innerObject = new BasicDBObject();
        innerObject.put(FIELD_KEY, MongoQueryHelper.getKeyBson(entry.getKey()));
        innerObject.put(FIELD_VALUE,
                supplier.createDBObject(entry.getValue())
        );
        innerObject.put(FIELD_SAVE_DATE, entry.getSaveDate());
        innerObject.put(FIELD_EXPIRE_TIMESTAMP, entry.getExpireTimestamp());
        return innerObject;
    }

    Bson getValueObject() {
        return (Bson) sourceObject.get(FIELD_VALUE);
    }

    void updateValue(V value) {
        if(id==null) {
            throw new IllegalStateException("update on not stored object");
        }
        this.entry.setOldValue(entry.getValue());
        this.entry.setValue(value);
        this.sourceObject.put(FIELD_VALUE,
                supplier.createDBObject(value)
        );
    }

    ObjectId getObjectId() {
        return id;
    }

    Long getExpireTimestamp() {
        return entry.getExpireTimestamp();
    }

}
