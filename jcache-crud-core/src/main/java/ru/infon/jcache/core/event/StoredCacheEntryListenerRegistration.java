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
import javax.cache.event.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An internal structure to represent the registration of a {@link CacheEntryListener}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @author Brian Oliver
 * @author KostaPC
 */
public class StoredCacheEntryListenerRegistration<K, V> {

    private final CacheEntryListenerConfiguration<K, V> configuration;
    private CacheEntryListener<? super K, ? super V> listener;
    private CacheEntryEventFilter<? super K, ? super V> filter;
    private boolean isOldValueRequired;
    private boolean isSynchronous;

    private Map<EventType, List<CacheEntryEvent<K, V>>> eventsBulk = new HashMap<>();

    /**
     * Constructs an {@link StoredCacheEntryListenerRegistration}.
     *
     * @param configuration  the {@link CacheEntryListenerConfiguration} to be registered
     */
    public StoredCacheEntryListenerRegistration(CacheEntryListenerConfiguration<K, V> configuration) {
        this.configuration = configuration;
        this.listener = configuration.getCacheEntryListenerFactory().create();
        this.filter = configuration.getCacheEntryEventFilterFactory() == null
                ? null
                : configuration.getCacheEntryEventFilterFactory().create();
        this.isOldValueRequired = configuration.isOldValueRequired();
        this.isSynchronous = configuration.isSynchronous();
    }

    /**
     * Invoke {@link CacheEntryListener} callback for event.
     *
     */
    public void store(CacheEntryEvent<K, V> event) {
        if(!filter(event)) {
            return;
        }
        List<CacheEntryEvent<K, V>> events = eventsBulk.get(event.getEventType());
        if(events==null) {
            events = new LinkedList<>();
            eventsBulk.put(event.getEventType(), events);
        }
        events.add(event);
    }

    public void evalute() {
        for (
                Map.Entry<EventType, List<CacheEntryEvent<K, V>>> eventsEntry :
                eventsBulk.entrySet())
        {
            EventListenerWrapper<K,V> wrapper = new EventListenerWrapper<>(
                    listener, eventsEntry.getKey(), eventsEntry.getValue()
            );
            //TODO: we need to handle exceptions here
            //TODO: we need to work out which events should be raised synchronously or asynchronously
            //TODO: we need to remove/hide old values appropriately
            try {
                wrapper.evolute();
            } catch (Throwable e) {
                if (!(e instanceof CacheEntryListenerException)) {
                    throw new CacheEntryListenerException("Exception on listener execution", e);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Evaluates specified {@link CacheEntryEvent}.
     *
     * @param event the event that occurred
     * @return true if the evaluation passes, otherwise false.
     *         The effect of returning true is that listener will be invoked
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    public boolean filter(CacheEntryEvent<? extends K, ? extends V> event) {
        return filter.evaluate(event);
    }

    /**
     * Determines if the old/previous value should to be supplied with the
     * {@link CacheEntryEvent}s dispatched to the
     * {@link CacheEntryListener}.
     */
    public boolean isOldValueRequired() {
        return isOldValueRequired;
    }

    /**
     * Determines if {@link CacheEntryEvent}s should be raised
     * synchronously.
     *
     * @return <code>true</code> if events should be raised synchronously
     */
    public boolean isSynchronous() {
        return isSynchronous;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + (isOldValueRequired ? 1231 : 1237);
        result = prime * result + (isSynchronous ? 1231 : 1237);
        result = prime * result
                + ((listener == null) ? 0 : listener.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (!(object instanceof StoredCacheEntryListenerRegistration)) {
            return false;
        }
        StoredCacheEntryListenerRegistration<?, ?> other = (StoredCacheEntryListenerRegistration<?, ?>) object;
        if (filter == null) {
            if (other.filter != null) {
                return false;
            }
        } else if (!filter.equals(other.filter)) {
            return false;
        }
        if (isOldValueRequired != other.isOldValueRequired) {
            return false;
        }
        if (isSynchronous != other.isSynchronous) {
            return false;
        }
        if (listener == null) {
            if (other.listener != null) {
                return false;
            }
        } else if (!listener.equals(other.listener)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the underlying configuration used to create this registration
     * @return the configuration
     */
    public CacheEntryListenerConfiguration<K, V> getConfiguration() {
        return configuration;
    }
}
