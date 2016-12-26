package ru.infon.jcache.core;

import javax.cache.processor.MutableEntry;

/**
 * 19.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class StorableMutableEntry<K, V> implements MutableEntry<K,V> {

    private final StorableEntryHandler<K,V> handler;

    private final K key;
    private V value;

    public StorableMutableEntry(StorableEntryHandler<K,V> handler,  K key, V value) {
        this.handler = handler;
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean exists() {
        return getValue()==null;
    }

    @Override
    public void remove() {
        handler.remove(getKey());
    }

    @Override
    public void setValue(V value) {
        handler.replace(getKey(), value);
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return clazz.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + clazz + " is not a supported by this implementation");
    }
}
