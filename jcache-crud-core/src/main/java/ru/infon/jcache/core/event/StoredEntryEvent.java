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

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;

/**
 * The reference implementation of the {@link CacheEntryEvent}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Greg Luck
 * @since 1.0
 */
public class StoredEntryEvent<K, V> extends CacheEntryEvent<K, V> {

    private K key;
    private V value;
    private V oldValue;
    private boolean oldValueAvailable;

    /**
     * Constructs a cache entry event from a given cache as source
     * (without an old value)
     *
     * @param source the cache that originated the event
     * @param entry  the entry
     */
    public StoredEntryEvent(Cache<K, V> source, Cache.Entry<K, V> entry, EventType eventType) {
        super(source, eventType);
        this.key = entry.getKey();
        this.value = entry.getValue();
        this.oldValue = null;
        this.oldValueAvailable = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public K getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getOldValue() throws UnsupportedOperationException {
        if (isOldValueAvailable()) {
            return oldValue;
        } else {
            throw new UnsupportedOperationException("Old value is not available for key");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz != null && clazz.isInstance(this)) {
            return (T) this;
        } else {
            throw new IllegalArgumentException("The class " + clazz + " is unknown to this implementation");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOldValueAvailable() {
        return oldValueAvailable;
    }
}
