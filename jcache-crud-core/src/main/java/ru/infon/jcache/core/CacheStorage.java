package ru.infon.jcache.core;

import java.net.URI;
import java.util.Properties;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
public abstract class CacheStorage {

    private URI uri;
    private Properties properties;

    public CacheStorage(URI uri, Properties properties) {
        this.uri = uri;
        this.properties = properties;
    }

    public URI getUri() {
        return uri;
    }

    public Properties getProperties() {
        return properties;
    }

    public abstract <K,V> CacheStorageSession<K,V> openSession(
            String cacheName, Class<K> keyType, Class<V> valueType
    );

    //public abstract <K,V> StorableEntry<K,V> createEntry(K key, V value);

    public abstract boolean isFixedTypes();
}
