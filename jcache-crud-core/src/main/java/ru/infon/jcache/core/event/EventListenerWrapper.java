package ru.infon.jcache.core.event;

import javax.cache.event.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class EventListenerWrapper<K, V> {


    private CacheEntryExpiredListener expiredListener = null;
    private CacheEntryCreatedListener createdListener = null;
    private CacheEntryUpdatedListener updatedListener = null;
    private CacheEntryRemovedListener removedListener = null;

    private Iterable<CacheEntryEvent<K, V>> events;

    EventListenerWrapper(
            CacheEntryListener<? super K, ? super V> listener,
            EventType eventType,
            Iterable<CacheEntryEvent<K, V>> events
    ) {
        this.events = events;
        switch (eventType) {
            case CREATED:
                if(listener instanceof CacheEntryCreatedListener) {
                    createdListener = (CacheEntryCreatedListener) listener;
                }
                break;
            case EXPIRED:
                if(listener instanceof CacheEntryExpiredListener) {
                    expiredListener = (CacheEntryExpiredListener) listener;
                }
                break;
            case REMOVED:
                if(listener instanceof CacheEntryRemovedListener) {
                    removedListener = (CacheEntryRemovedListener) listener;
                }
                break;
            case UPDATED:
                if(listener instanceof CacheEntryUpdatedListener) {
                    updatedListener = (CacheEntryUpdatedListener) listener;
                }
                break;
        }
    }

    @SuppressWarnings("unchecked")
    void evolute() {
        if(expiredListener!=null) expiredListener.onExpired(events);
        if(createdListener!=null) createdListener.onCreated(events);
        if(updatedListener!=null) updatedListener.onUpdated(events);
        if(removedListener!=null) removedListener.onRemoved(events);
    }


}
