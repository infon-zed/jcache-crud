package ru.infon.jcache.core;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

/**
 * 24.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class PolicyExecutor<K,V> implements ExpiryPolicy {

    private final ExpiryPolicy policy;
    private final CacheStorageSession<K,V> storage;

    public PolicyExecutor(CacheStorageSession<K,V> storage, ExpiryPolicy policy) {
        this.policy = policy;
        this.storage = storage;
    }

    StorableEntry<K,V> execute(StorableEntry<K,V> cacheEntity, Action action) {
        long now = System.currentTimeMillis();
        if(cacheEntity==null) {
            return null;
        }

        if(cacheEntity.isExpired()) {
            storage.remove(cacheEntity.getKey());
            return null;
        }

        Duration duration;
        switch (action) {
            case CHECK:
                return cacheEntity;
            /*
             * This method is called by a caching implementation after a Cache.Entry is
             * created, but before a Cache.Entry is added to a cache
             */
            case CREATE:
                duration = policy.getExpiryForCreation();
                break;
            /*
             * This method is called by a caching implementation after a Cache.Entry is
             * accessed to determine the {@link Duration} before an entry expires.
             */
            case ACCESS:
                duration = policy.getExpiryForAccess();
                break;
            /*
             * This method is called by the caching implementation after a Cache.Entry is
             * updated to determine the {@link Duration} before the updated entry expires.
             */
            case UPDATE:
                duration = policy.getExpiryForUpdate();
                break;
            default:
                return cacheEntity;
        }

        if(duration==null) {
            return cacheEntity;
        }

        if(duration.isZero()) {
            storage.remove(cacheEntity.getKey());
            return null;
        }

        long expireTime = duration.getAdjustedTime(now);
        cacheEntity.setExpireTimestamp(expireTime);

        if(cacheEntity.isExpired()) {
            storage.remove(cacheEntity.getKey());
            return null;
        }
        storage.updateExpireTime(cacheEntity);

        return cacheEntity;
    }

    @Override
    public Duration getExpiryForCreation() {
        return policy.getExpiryForCreation();
    }

    @Override
    public Duration getExpiryForAccess() {
        return policy.getExpiryForAccess();
    }

    @Override
    public Duration getExpiryForUpdate() {
        return policy.getExpiryForUpdate();
    }

    enum Action {
        CREATE,
        ACCESS,
        UPDATE,
        CHECK
    }

}
