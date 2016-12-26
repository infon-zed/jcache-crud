package ru.infon.jcache.mongo.basic;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

/**
 * 21.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class CacheMongoObject<K,V> {

    public static final String FIELD_KEY = "key";
    public static final String FIELD_VALUE = "value";

    @Id
    private ObjectId id;
    @Indexed @Property(FIELD_KEY)
    private K key;
    @Property(FIELD_VALUE)
    private V value;

    public CacheMongoObject(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public CacheMongoObject() {
    }

    public ObjectId getId() {
        return id;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheMongoObject)) return false;

        CacheMongoObject<?, ?> that = (CacheMongoObject<?, ?>) o;

        if (!id.equals(that.id)) return false;
        if (!key.equals(that.key)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
