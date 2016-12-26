package ru.infon.jcache.core;

import java.util.Collection;
import java.util.Set;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
// TODO: separate interface for CRUD operations only
public abstract class CacheStorageSession<K, V> {

    private final CacheStorage cacheStorage;
    private final EntryFactory<K,V> entryFactory;

    public CacheStorageSession(CacheStorage cacheStorage, Class<K> keyType, Class<V> valueType) {
        this.cacheStorage = cacheStorage;
        this.entryFactory = new EntryFactory<>(keyType, valueType);
    }

    public CacheStorage getCacheStorage() {
        return cacheStorage;
    }

    public final EntryFactory<K,V> getEntryFactory() {
        return entryFactory;
    }

    public abstract boolean containsKey(K key);

    public abstract StorableEntry<K, V> get(K key);
    public abstract Collection<StorableEntry<K,V>> getAll(Set<? extends K> keys);
    public abstract Collection<StorableEntry<K,V>> getAll();

    public abstract void put(StorableEntry<K, V> entry);
    public abstract void putAll(Collection<StorableEntry<K, V>> map);

    public abstract boolean update(StorableEntry<K, V> entry);
    public abstract void updateExpireTime(StorableEntry<K, V> entry);

    public abstract boolean remove(K key);
    public abstract void removeAll(Set<? extends K> keys);
    public abstract void removeAll();

    public abstract void close();
    public abstract boolean isClosed();

}
