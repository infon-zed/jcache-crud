package ru.infon.jcache.mongo;

import com.mongodb.DBObject;
import ru.infon.jcache.core.StorableEntry;

/**
 * 10.11.2016
 * @author kostapc
 * 2016 Infon
 */
interface MongoEntrySupplier<K,V> {
    StorableEntry<K,V> createEntry(K key, V value);
    DBObject createDBObject(V value);
    V parseValueDBObject(DBObject valueDBObject);
}
