package ru.infon.jcache.mongo;


import ru.infon.jcache.core.CacheStorage;
import ru.infon.jcache.core.CacheStorageSession;

import java.net.URI;
import java.util.Properties;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoCacheStorage extends CacheStorage {

    private MongoConnection mongoConnection;

    public MongoCacheStorage(URI uri, Properties properties) {
        super(uri, properties);
        mongoConnection = new MongoConnection(
                new MongoURIWrapper(uri), properties
        );
    }

    @Override
    public <K, V> CacheStorageSession<K, V> openSession(
            String cacheCollectionName, Class<K> keyType, Class<V> valueType
    ) {
        return new MongoStorageSession<>(
                this, cacheCollectionName, mongoConnection,
                keyType, valueType
        );
    }

    @Override
    public boolean isFixedTypes() {
        return false;
    }
}
