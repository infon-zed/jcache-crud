package ru.infon.jcache.core;

/**
 * 19.10.2016
 * @author kostapc
 * 2016 Infon
 */
public interface StorableEntryHandler<K,V> {
    boolean remove(K key);
    boolean replace(K key, V newValue);
}
