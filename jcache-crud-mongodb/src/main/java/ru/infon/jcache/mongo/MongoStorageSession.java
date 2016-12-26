package ru.infon.jcache.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import ru.infon.jcache.core.CacheStorage;
import ru.infon.jcache.core.CacheStorageSession;
import ru.infon.jcache.core.StorableEntry;
import org.bson.conversions.Bson;

import java.util.*;


/**
 * 19.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoStorageSession<K,V> extends CacheStorageSession<K,V> implements MongoEntrySupplier<K,V> {

    private final String collection;
    private final MongoConnection mongo;

    private final Class<K> keyType;
    private final Class<V> valueType;

    private boolean isClosed;

    public MongoStorageSession(
            CacheStorage cacheStorage,
            String collectionName,
            MongoConnection mongo,
            Class<K> keyType, Class<V> valueType) {
        super(cacheStorage, keyType, valueType);
        this.collection = collectionName;
        this.mongo = mongo;
        this.keyType = keyType;
        this.valueType = valueType;
        this.isClosed = false;
        if(!mongo.getMorphia().isMapped(valueType)) {
            try {
                // hack to force moprhia to map class
                // if using generic MongoCacheEntity with @id - random problems with object mapping
                mongo.getMorphia().toDBObject(valueType.newInstance());
                mongo.getMorphia().toDBObject(keyType.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "reflection instaniation of object not possible (maybe no default constuctor?)" ,
                e);
            }
        }
        // TODO: ensure indexes existed, create with collection
    }

    @Override
    public boolean containsKey(K key) {
        assertOpened();
        Bson query = MongoQueryHelper.createFindQuery(key);
        return mongo.getMongoDB().getCollection(collection).count(query)>0;
    }

    private MongoCacheEntity<K,V> findCacheEntity(K key) {
        Bson query = MongoQueryHelper.createFindQuery(key);
        FindIterable<DBObject> list = mongo.getMongoDB().getCollection(collection, DBObject.class).find(query);
        DBObject object = list.first();
        if(object==null) {
            return null;
        }
        return new MongoCacheEntity<>(
                object, keyType, valueType, this
        );
    }

    @Override //tested
    public StorableEntry<K, V> get(K key) {
        assertOpened();
        MongoCacheEntity<K,V> cacheEntity = findCacheEntity(key);
        if(cacheEntity==null) {
            return null;
        }
        updateRecord(
                cacheEntity.getFindQuery(),
                MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                cacheEntity.getExpireTimestamp()
        );
        return cacheEntity.getStorableEntiry();
    }

    private Collection<StorableEntry<K, V>> getAll(Bson query) {
        assertOpened();
        FindIterable<DBObject> list = mongo.getMongoDB().getCollection(collection, DBObject.class).find(query);
        List<StorableEntry<K, V>> entries = new LinkedList<>();

        BasicDBList in = new BasicDBList();
        Long lastExpireDate = null;

        for (DBObject dbObject : list) {
            MongoCacheEntity<K,V> cacheEntity = new MongoCacheEntity<>(
                    dbObject, keyType, valueType, this
            );
            lastExpireDate = cacheEntity.getExpireTimestamp();
            in.add(cacheEntity.getObjectId());
            entries.add(
                    cacheEntity.getStorableEntiry()
            );
        }

        if(lastExpireDate!=null) {
            BasicDBObject inQuery = new BasicDBObject("$in", in);
            inQuery = new BasicDBObject(MongoCacheEntity.FIELD_MONGO_ID, inQuery);
            updateRecord(
                    inQuery,
                    MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                    lastExpireDate
            );
        }

        return entries;
    }

    @Override // tested
    public Collection<StorableEntry<K, V>> getAll(Set<? extends K> keys) {
        assertOpened();
        Bson query = MongoQueryHelper.createFindQuery(keys);
        return  getAll(query);
    }

    @Override // tested
    public Collection<StorableEntry<K, V>> getAll() {
        assertOpened();
        Bson query = new BasicDBObject();
        return getAll(query);
    }

    private MongoCacheEntity<K,V> create(StorableEntry<K, V> entry) {
        return new MongoCacheEntity<>(
                entry, this
        );
    }

    @Override
    public void updateExpireTime(StorableEntry<K, V> entry) {
        updateRecord(
                MongoQueryHelper.createFindQuery(entry.getKey()),
                MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                entry.getExpireTimestamp()
        );
    }

    @Override
    public boolean update(StorableEntry<K, V> entry) {
        return update(entry, null);
    }

    private boolean update(StorableEntry<K, V> entry, MongoCacheEntity<K,V> oldCachedEntry) {
        assertOpened();
        if(oldCachedEntry==null) {
            oldCachedEntry = findCacheEntity(entry.getKey());
            if(oldCachedEntry==null) {
                return false;
            }
        }
        oldCachedEntry.updateValue(entry.getValue());
        updateRecord(
                oldCachedEntry.getFindQuery(),
                MongoCacheEntity.FIELD_VALUE,
                oldCachedEntry.getValueObject(),
                MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                oldCachedEntry.getExpireTimestamp()
        );
        return true;
    }

    private void updateRecord(Bson query, Object... values) {
        BasicDBObject updates = new BasicDBObject();
        for (int i = 0; i < values.length; i+=2) {
            updates.put(
                values[i].toString(),
                values[i+1]
            );
        }
        mongo.getMongoDB().getCollection(collection).updateOne(
                query,
                new BasicDBObject("$set", updates)
        );
    }

    @Override // TODO: test replace if object exists
    public void put(StorableEntry<K, V> entry) {
        assertOpened();
        MongoCacheEntity<K,V> cacheEntity = findCacheEntity(entry.getKey());
        if (cacheEntity == null) { // create
            cacheEntity = create(entry);
            DBObject cacheObject  = cacheEntity.getDBObject();
            mongo.getMongoDB().getCollection(collection, DBObject.class).insertOne(cacheObject);
        } else {
            //throw new IllegalStateException("cache entry with key \""+entry.getKey()+"\" already exists");
            updateRecord(
                    cacheEntity.getFindQuery(),
                    MongoCacheEntity.FIELD_VALUE,
                    createDBObject(entry.getValue()),
                    MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                    cacheEntity.getExpireTimestamp()
            );
        }
    }

    @Override // TODO: test replace with existed records
    public void putAll(Collection<StorableEntry<K, V>> keys) {
        assertOpened();
        Map<K,  StorableEntry<K, V>> newObjects = new HashMap<>(keys.size());
        for (StorableEntry<K, V> storableEntry : keys) {
            newObjects.put(
                    storableEntry.getKey(),
                    storableEntry
            );
        }
        Map<K,DBObject> presentObjects = new HashMap<>();
        Bson query = MongoQueryHelper.createFindQuery(newObjects.keySet());
        FindIterable<DBObject> list = mongo.getMongoDB().getCollection(collection, DBObject.class).find(query);
        for (DBObject object : list) { // key present in database
            K key = keyType.cast(object.get(MongoCacheEntity.FIELD_KEY));
            presentObjects.put(key, object);
        }

        List<DBObject> objects = new ArrayList<>(keys.size());
        for (Map.Entry<K, StorableEntry<K, V>> newEntry : newObjects.entrySet()) {
            DBObject oldObject = presentObjects.get(newEntry.getKey());
            MongoCacheEntity<K,V> newCacheEntry = new MongoCacheEntity<>(
                    newEntry.getValue(), this
            );
            if(oldObject!=null) {
                updateRecord(
                        new BasicDBObject(MongoCacheEntity.FIELD_MONGO_ID, oldObject.get(MongoCacheEntity.FIELD_MONGO_ID)),
                        MongoCacheEntity.FIELD_VALUE,
                        createDBObject(newEntry.getValue().getValue()),
                        MongoCacheEntity.FIELD_EXPIRE_TIMESTAMP,
                        newCacheEntry.getExpireTimestamp()
                );
               continue;
            }
            objects.add(newCacheEntry.getDBObject());
        }

        mongo.getMongoDB().getCollection(collection, DBObject.class).insertMany(objects);
    }

    @Override
    public boolean remove(K key) {
        assertOpened();
        Bson query = MongoQueryHelper.createFindQuery(key);
        DeleteResult result = mongo.getMongoDB().getCollection(collection, DBObject.class).deleteOne(query);
        return result.getDeletedCount()>0;
    }

    @Override // tester
    public void removeAll(Set<? extends K> keys) {
        assertOpened();
        Bson query = MongoQueryHelper.createFindQuery(keys);
        mongo.getMongoDB().getCollection(collection, DBObject.class).deleteMany(query);
    }

    @Override
    public void removeAll() {
        assertOpened();
        Bson query = new BasicDBObject();
        mongo.getMongoDB().getCollection(collection, DBObject.class).deleteMany(query);
    }

    private void assertOpened() {
        if(isClosed) {
            throw new IllegalStateException("session is closed!");
        }
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public StorableEntry<K, V> createEntry(K key, V value) {
        return getEntryFactory().create(key, value);
    }

    @Override
    public DBObject createDBObject(V value) {
        return mongo.getMorphia().toDBObject(value);
    }

    @Override
    public V parseValueDBObject(DBObject valueDBObject) {
        return mongo.getMorphia().fromDBObject(mongo.getDatastore(), valueType, valueDBObject);
    }
}
