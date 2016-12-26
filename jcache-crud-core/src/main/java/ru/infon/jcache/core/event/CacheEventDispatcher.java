/**
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
package ru.infon.jcache.core.event;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Collects and appropriately dispatches {@link CacheEntryEvent}s to
 * {@link CacheEntryListener}s.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @author Brian Oliver
 * @author KostaPC
 */
public class CacheEventDispatcher<K, V> {

    private final CopyOnWriteArrayList<StoredCacheEntryListenerRegistration<K, V>> listeners;
    private final Executor executor;

    /**
     * Constructs an {@link CacheEventDispatcher}.
     */
    public CacheEventDispatcher(Executor executor) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.executor = executor;
    }

    /**
     * Requests that the specified event be prepared for dispatching to the
     * specified type of listeners.
     *
     * @param event         the event to be dispatched
     */
    public void storeEvent(CacheEntryEvent<K, V> event) {
        if (event == null) {
            throw new NullPointerException("event can't be null");
        }

        for (StoredCacheEntryListenerRegistration<K, V> listener : listeners) {
            listener.store(event);
        }

    }

    public void register(CacheEntryListenerConfiguration<K, V> listenerConfiguration) {
        StoredCacheEntryListenerRegistration<K, V> registration =
                new StoredCacheEntryListenerRegistration<>(listenerConfiguration);
        listeners.add(registration);
    }

    public void unregister(CacheEntryListenerConfiguration<K, V> listenerConfiguration) {
        for (StoredCacheEntryListenerRegistration<K, V> listenerRegistration : listeners) {
            if (listenerConfiguration.equals(listenerRegistration.getConfiguration())) {
                listeners.remove(listenerRegistration);
            }
        }
    }

    /**
     * Dispatches the added events to the listeners defined by the specified
     * {@link CacheEntryListenerConfiguration}s.
     *
     */
    public void dispatch() {
        for (final StoredCacheEntryListenerRegistration<K, V> listener : listeners) {
            if(listener.isSynchronous()) {
                listener.evalute();
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.evalute();
                    }
                });
            }
        }
    }


}
