/*
 * --- original source copyright and license header ---
 *
 * Copyright 2011-2013 Terracotta, Inc.
 * Copyright 2011-2013 Oracle America Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.infon.jcache.core;

import ru.infon.jcache.core.spi.StoredCachingProvider;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class StoredCacheManager implements CacheManager, Executor {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");

    private final StoredCachingProvider cachingProvider;

    private final URI uri;
    private final ClassLoader classLoader;
    private final Properties properties;

    private final CacheStorage storage;
    private final CacheRepository repository;

    private volatile boolean isClosed;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    /**
     * Constructs a new StoredCacheManager with the specified name.
     *
     * @param cachingProvider the CachingProvider that created the CacheManager
     * @param uri             the name of this cache manager
     * @param classLoader     the ClassLoader that should be used in converting values into Java Objects.
     * @param properties      the vendor specific Properties for the CacheManager
     * @throws NullPointerException if the URI and/or classLoader is null.
     */
    public StoredCacheManager(StoredCachingProvider cachingProvider, URI uri, ClassLoader classLoader, Properties properties) {

        if (classLoader == null) {
            throw new NullPointerException("No ClassLoader specified");
        }
        if (uri == null) {
            throw new NullPointerException("No CacheManager URI specified");
        }
        if (cachingProvider == null) {
            throw new NullPointerException("No CachingProvider specified");
        }
        this.uri = uri;
        this.cachingProvider = cachingProvider;
        this.classLoader = classLoader;
        this.properties = properties == null ? new Properties() : new Properties(properties);
        this.isClosed = false;
        this.storage = cachingProvider.createStorageObject(uri, properties);
        this.repository = new CacheRepository(storage.isFixedTypes());
    }

    CacheStorage getStorage() {
        return storage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        //first releaseCacheManager the CacheManager from the CacheProvider so that
        //future requests for this CacheManager won't return this one
        cachingProvider.releaseCacheManager(getURI(), getClassLoader());

        isClosed = true;
        for (StoredCache<?, ?> cache : repository) {
            try {
                cache.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping cache: " + cache, e);
            }
        }

        //attempt to shutdown (and wait for the cache to shutdown)
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException("cache name cannot be null");
        }
        if (configuration == null) {
            throw new NullPointerException("configuration must not be null");
        }

        if(
            configuration.getKeyType() == null ||
            configuration.getValueType() == null
        ) {
            throw new NullPointerException(String.format(
                    "configuration must contains key and value types. present key: %s and value %s",
                    configuration.getKeyType(), configuration.getValueType()
            ));
        }
        if(repository.checkCacheExists(cacheName, configuration.getKeyType(), configuration.getValueType())) {
            throw new CacheException(String.format(
                    "cache \"%s\" with present parameters <%s,%s> already exists (fixed types: %s)",
                    cacheName, configuration.getKeyType(), configuration.getValueType(), storage.isFixedTypes()
            ));
        }
        StoredCache<K,V> cache = new StoredCache<>(this, cacheName, getClassLoader(), configuration);
        repository.register(cache);
        return cache;

    }

    /**
     * {@inheritDoc}
     */
    @Override
        public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (keyType == null) {
            throw new NullPointerException("keyType can not be null");
        }
        if (valueType == null) {
            throw new NullPointerException("valueType can not be null");
        }
        if (cacheName == null) {
            throw new NullPointerException("cache name cannot be null");
        }

        return repository.get(cacheName, keyType, valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException("CacheManager is closed");
        }
        if (cacheName == null) {
            throw new NullPointerException("cache name cannot be null");
        }

        return repository.get(cacheName, null, null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<String> getCacheNames() {
        if (isClosed()) {
            throw new IllegalStateException("CacheManager is closed");
        }
        return repository.getNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException("CacheManager is closed");
        }
        if (cacheName == null) {
            throw new NullPointerException("cache name cannot be null");
        }

        Iterable<StoredCache> caches = repository.unregister(cacheName);

        for (StoredCache cache : caches) {
            try {
                cache.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping cache: " + cache, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        // TODO: enable statistic
        //((StoredCache)caches.get(cacheName)).setStatisticsEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        // TODO: enable managment
        //((StoredCache)caches.get(cacheName)).setManagementEnabled(enabled);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return clazz.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + clazz + " is not a supported by this implementation");
    }

    @Override
    public void execute(Runnable task) {
        if(task==null) {
            throw new NullPointerException("task cannot be null");
        }
        executorService.execute(task);
    }

}
