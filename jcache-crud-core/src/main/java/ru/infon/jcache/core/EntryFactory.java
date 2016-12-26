package ru.infon.jcache.core;

import java.util.Date;

/**
 * 10.11.2016
 * @author kostapc
 * 2016 Infon
 */
public class EntryFactory<K,V> {

    private final Class<K> keyType;
    private final Class<V> valueType;

    public EntryFactory(Class<K> keyType, Class<V> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public StorableEntry<K,V> create(K key, V value) {
        return create(key, value, null);
    }

    public StorableEntry<K,V> create(K key, V value, V oldValue) {
        StorableEntry<K,V> entry = new StorableEntry<>(key, value, oldValue, keyType, valueType);
        entry.setSaveDate(new Date());
        return entry;
    }

}
