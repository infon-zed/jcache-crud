package ru.infon.jcache.core;

import javax.cache.CacheManager;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public abstract class StoredCacheMXBean {

    public CacheManager getCacheManager() {
        return null;
    }

    public String getName() {
        return null;
    }
}
