
package ru.infon.jcache.mongo.spi;

import ru.infon.jcache.core.CacheStorage;
import ru.infon.jcache.core.spi.StoredCachingProvider;
import ru.infon.jcache.mongo.MongoCacheStorage;

import javax.cache.CacheException;
import javax.cache.configuration.OptionalFeature;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class MongoCachingProvider extends StoredCachingProvider {

    @Override
    public CacheStorage createStorageObject(URI uri, Properties properties) {
        return new MongoCacheStorage(uri, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getDefaultURI() {
        try {
            // mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
            final String localhostNoAuthCache = "mongodb://localhost:27017/jcache";
            return new URI(localhostNoAuthCache);
        } catch (URISyntaxException e) {
            throw new CacheException(
                    "Failed to create the default URI for the javax.cache Reference Implementation",
            e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        switch (optionalFeature) {

            case STORE_BY_REFERENCE:
                return false;

            default:
                return false;
        }
    }
}
