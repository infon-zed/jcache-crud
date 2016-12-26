package ru.infon.jcache.core;

import javax.cache.Cache;
import java.util.Date;
import java.util.Map;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class StorableEntry<K,V> implements Cache.Entry<K,V> {

    private Date saveDate;
    private Long expireTimestamp;

    private K key;
    private V value;
    private V oldValue;

    private Class<K> keyType;
    private Class<V> valueType;

    protected StorableEntry(K key, V value, V oldValue, Class<K> keyType, Class<V> valueType) {
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void setOldValue(V oldValue) {
        this.oldValue = oldValue;
    }

    public Date getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(Date saveDate) {
        this.saveDate = saveDate;
    }

    public Long getExpireTimestamp() {
        return expireTimestamp;
    }

    public void setExpireTimestamp(Long expireTimestamp) {
        this.expireTimestamp = expireTimestamp;
    }

    public Class<K> getKeyType() {
        return keyType;
    }

    public Class<V> getValueType() {
        return valueType;
    }

    public boolean isExpired() {
        if(expireTimestamp==null) {
            return false;
        }
        return expireTimestamp <= System.currentTimeMillis();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return clazz.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + clazz + " is not a supported by this implementation");
    }
}
