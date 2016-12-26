package ru.infon.jcache.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 20.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class CacheRepository implements Iterable<StoredCache<?,?>> {
    private final Map<CacheHash, StoredCache<?,?>> caches = new ConcurrentHashMap<>();
    private final Map<String, List<CacheHash>> cacheRegistry = new ConcurrentHashMap<>();
    private final boolean isFixedTypes;

    public CacheRepository(boolean isFixedTypes) {
        this.isFixedTypes = isFixedTypes;
    }

    public synchronized <K,V> boolean checkCacheExists(String cacheName, Class<K> keyType, Class<V> valueType) {
        if (isFixedTypes) {
            return cacheRegistry.containsKey(cacheName);
        } else {
            CacheHash hash = new CacheHash(
                    cacheName, keyType, valueType
            );
            return caches.containsKey(hash);
        }
    }

    /**
     * call checkCacheExists before registration cache object
     * @param cache - just created cache object
     * @param <K> - key type
     * @param <V> - value type
     */
    public synchronized  <K,V> void register(StoredCache<K,V> cache) {
        CacheHash hash = new CacheHash(
                cache.getName(), cache.getKeyClass(), cache.getValueClass()
        );
        // WARNING: possible rewriting object in multithreading
        List<CacheHash> hashes = cacheRegistry.get(hash.getName());
        if(hashes==null) {
            hashes = new LinkedList<>();
            cacheRegistry.put(cache.getName(), hashes);
        }
        hashes.add(hash);
        caches.put(hash, cache);
    }

    public synchronized Iterable<StoredCache> unregister(String cacheName) {
        List<StoredCache> cachesByName = new LinkedList<>();
        List<CacheHash> hashes = cacheRegistry.get(cacheName);
        cacheRegistry.remove(cacheName);
        for (CacheHash hash : hashes) {
            cachesByName.add(caches.get(hash));
            caches.remove(hash);
        }
        return cachesByName;
    }

    public synchronized <K,V> StoredCache<K,V> get(String cacheName, Class<K> keyType, Class<V> valueType) {
        CacheHash hash;
        if(isFixedTypes || keyType==null || valueType==null) {
            List<CacheHash> hashes = cacheRegistry.get(cacheName);
            if(hashes==null) {
                return null;
            }
            if(hashes.size()>1) {
                throw new IllegalStateException(String.format(
                        "more than one cache for name  \"%s\" present (isFixedTypes: %s). " +
                        "Use getCache(String cacheName, Class<K> keyType, Class<V> valueType)",
                        cacheName, isFixedTypes
                ));
            }
            if(hashes.size()==0) {
                return null;
            }
            hash = hashes.get(0);
        } else {
            hash = new CacheHash(cacheName, keyType, valueType);
        }
        //noinspection unchecked
        return (StoredCache<K, V>) caches.get(hash);
    }

    public Iterable<String> getNames() {
        return Collections.unmodifiableCollection(
                cacheRegistry.keySet()
        );
    }

    @Override
    public Iterator<StoredCache<?, ?>> iterator() {
        return caches.values().iterator();
    }


}
