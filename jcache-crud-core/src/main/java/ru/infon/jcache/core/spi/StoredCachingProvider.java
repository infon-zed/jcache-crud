package ru.infon.jcache.core.spi;

import ru.infon.jcache.core.CacheStorage;
import ru.infon.jcache.core.StoredCacheManager;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.HashMap;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 * Implementation of the {@link CachingProvider}.
 * 20.10.2016
 * @author KostaPC
 * 2016 Infon
 */
public abstract class StoredCachingProvider implements CachingProvider {

    /**
     * The CacheManagers scoped by ClassLoader and URI.
     */
    private final WeakHashMap<ClassLoader, HashMap<URI, CacheManager>> cacheManagersByClassLoader;

    private final static Properties defaultProperties = new Properties();

    /**
     * Constructs an MongoCachingProvider.
     */
    public StoredCachingProvider() {
        this.cacheManagersByClassLoader = new WeakHashMap<>();
    }

    /**
     * {@inheritDoc}
     * MongoDB connection URI: mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     */
    @Override
    public synchronized CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
        URI managerURI = uri == null ? getDefaultURI() : uri;
        ClassLoader managerClassLoader = classLoader == null ? getDefaultClassLoader() : classLoader;
        Properties managerProperties = properties == null ? defaultProperties : properties;

        HashMap<URI, CacheManager> cacheManagersByURI = cacheManagersByClassLoader.get(managerClassLoader);

        if (cacheManagersByURI == null) {
            cacheManagersByURI = new HashMap<>();
        }

        CacheManager cacheManager = cacheManagersByURI.get(managerURI);

        if (cacheManager == null) {
            cacheManager = new StoredCacheManager(this, managerURI, managerClassLoader, managerProperties);

            cacheManagersByURI.put(managerURI, cacheManager);
        }

        if (!cacheManagersByClassLoader.containsKey(managerClassLoader)) {
            cacheManagersByClassLoader.put(managerClassLoader, cacheManagersByURI);
        }

        return cacheManager;
    }

    /**
     * {@inheritDoc}
     * MongoDB connection URI: mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     */
    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
        return getCacheManager(uri, classLoader, getDefaultProperties());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager getCacheManager() {
        return getCacheManager(getDefaultURI(), getDefaultClassLoader(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getDefaultClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getDefaultProperties() {
        return defaultProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        WeakHashMap<ClassLoader, HashMap<URI, CacheManager>> managersByClassLoader = this.cacheManagersByClassLoader;
        this.cacheManagersByClassLoader.clear();

        for (ClassLoader classLoader : managersByClassLoader.keySet()) {
            for (CacheManager cacheManager : managersByClassLoader.get(classLoader).values()) {
                cacheManager.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close(ClassLoader classLoader) {
        ClassLoader managerClassLoader = classLoader == null ? getDefaultClassLoader() : classLoader;

        HashMap<URI, CacheManager> cacheManagersByURI = cacheManagersByClassLoader.remove(managerClassLoader);

        if (cacheManagersByURI != null) {
            for (CacheManager cacheManager : cacheManagersByURI.values()) {
                cacheManager.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close(URI uri, ClassLoader classLoader) {
        URI managerURI = uri == null ? getDefaultURI() : uri;
        ClassLoader managerClassLoader = classLoader == null ? getDefaultClassLoader() : classLoader;

        HashMap<URI, CacheManager> cacheManagersByURI = cacheManagersByClassLoader.get(managerClassLoader);
        if (cacheManagersByURI != null) {
            CacheManager cacheManager = cacheManagersByURI.remove(managerURI);

            if (cacheManager != null) {
                cacheManager.close();
            }

            if (cacheManagersByURI.size() == 0) {
                cacheManagersByClassLoader.remove(managerClassLoader);
            }
        }
    }

    /**
     * Releases the CacheManager with the specified URI and ClassLoader
     * from this CachingProvider.  This does not close the CacheManager.  It
     * simply releases it from being tracked by the CachingProvider.
     * <p>
     * This method does nothing if a CacheManager matching the specified
     * parameters is not being tracked.
     * </p>
     *
     * @param uri         the URI of the CacheManager
     * @param classLoader the ClassLoader of the CacheManager
     */
    public synchronized void releaseCacheManager(URI uri, ClassLoader classLoader) {
        URI managerURI = uri == null ? getDefaultURI() : uri;
        ClassLoader managerClassLoader = classLoader == null ? getDefaultClassLoader() : classLoader;

        HashMap<URI, CacheManager> cacheManagersByURI = cacheManagersByClassLoader.get(managerClassLoader);
        if (cacheManagersByURI != null) {
            cacheManagersByURI.remove(managerURI);

            if (cacheManagersByURI.size() == 0) {
                cacheManagersByClassLoader.remove(managerClassLoader);
            }
        }
    }

    public abstract CacheStorage createStorageObject(URI uri, Properties properties);

}
