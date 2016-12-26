package ru.infon.jcache.core;

import ru.infon.jcache.core.event.CacheEventDispatcher;
import ru.infon.jcache.core.event.StoredEntryEvent;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.EventType;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.*;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class StoredCache<K, V> implements Cache<K, V>, StorableEntryHandler<K,V> {

    private final String cacheName;
    private final MutableConfiguration<K, V> configuration;
    private final StoredCacheManager manager;
    private final ClassLoader classLoader; // TODO: implement classLoader usage
    private final CacheStorageSession<K, V> storage;
    private final EntryFactory<K,V> entryFactory;

    private final ProxyStorage<K, V> proxyStorage;
    private final CacheEventDispatcher<K,V> eventsDispatcher;

    private final PolicyExecutor<K,V> policyExecutor;

    public StoredCache(
            StoredCacheManager storedCacheManager,
            String cacheName, ClassLoader classLoader,
            Configuration<K,V> configuration
    ) {
        this.cacheName = cacheName;
        this.manager = storedCacheManager;
        this.classLoader = classLoader;

        //we make a copy of the configuration here so that the provided one
        //may be changed and or used independently for other caches.  we do this
        //as we don't know if the provided configuration is mutable
        if (configuration instanceof CompleteConfiguration) {
            //support use of CompleteConfiguration
            this.configuration = new MutableConfiguration<>((CompleteConfiguration<K, V>) configuration);
        } else {
            //support use of Basic Configuration
            this.configuration = new MutableConfiguration<>((CompleteConfiguration<K, V>) configuration);
            this.configuration.setStoreByValue(configuration.isStoreByValue());
            this.configuration.setTypes(configuration.getKeyType(), configuration.getValueType());
        }
        this.proxyStorage = new ProxyStorage<>(this.configuration);
        this.eventsDispatcher = new CacheEventDispatcher<>(manager);

        this.storage = storedCacheManager.getStorage().openSession(
                cacheName,
                this.configuration.getKeyType(),
                this.configuration.getValueType()
        );

        this.policyExecutor = new PolicyExecutor<>(
                storage, this.configuration.getExpiryPolicyFactory().create()
        );
        this.entryFactory = storage.getEntryFactory();
    }

    Class<K> getKeyClass() {
        return configuration.getKeyType();
    }

    Class<V> getValueClass() {
        return configuration.getValueType();
    }

    @Override
    public V get(K key) {
        assertOpened();
        StorableEntry<K, V> entry = storage.get(key);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.ACCESS);
        if(entry==null) {
            return null;
        }
        return proxyStorage.proxyLoad(key, entry.getValue());
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        assertOpened();
        Map<K, V> map = new HashMap<>();
        Collection<StorableEntry<K, V>> entries = storage.getAll(keys);
        for (StorableEntry<K, V> entry : entries) {
            entry = policyExecutor.execute(entry, PolicyExecutor.Action.ACCESS);
            if(entry==null) {
                continue;
            }
            map.put(entry.getKey(), entry.getValue());
        }
        // TODO: check is expired entry should be loaded by "loader"
        return proxyStorage.proxytLoadAll(keys, map);
    }

    @Override
    public boolean containsKey(K key) {
        assertOpened();
        return storage.containsKey(key);
    }

    @Override
    public void put(K key, V value) {
        assertOpened();
        StorableEntry<K, V> entry = entryFactory.create(key, value);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.CREATE);
        if(entry==null) { // rare case: already expired, not updating
            return;
        }
        storage.put(entry);
        proxyStorage.proxyWrite(entry);
        eventsDispatcher.storeEvent(
                new StoredEntryEvent<>(this,entry, EventType.CREATED)
        );
        eventsDispatcher.dispatch();
    }

    @Override
    public V getAndPut(K key, V value) {
        assertOpened();
        V oldValue = get(key);
        StorableEntry<K, V> entry = entryFactory.create(key, value, oldValue);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.UPDATE);
        if(entry==null) {
            return oldValue;
        }
        storage.update(entry);
        proxyStorage.proxyWrite(entry);
        eventsDispatcher.storeEvent(
                new StoredEntryEvent<>(this, entry, EventType.UPDATED)
        );
        eventsDispatcher.dispatch();
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        assertOpened();
        Collection<StorableEntry<K, V>> collection = new ArrayList<>(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            StorableEntry<K,V> storableEntry = entryFactory.create(
                    entry.getKey(), entry.getValue()
            );
            storableEntry = policyExecutor.execute(storableEntry, PolicyExecutor.Action.CREATE);
            if(storableEntry==null) {
                continue;
            }
            collection.add(storableEntry);
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(this, storableEntry, EventType.CREATED)
            );
        }
        collection = Collections.unmodifiableCollection(collection);
        storage.putAll(collection);
        proxyStorage.proxyWriteAll(collection);
        eventsDispatcher.dispatch();
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        assertOpened();
        if (storage.containsKey(key)) {
            return false;
        }
        put(key, value);
        return true;
    }

    @Override
    public boolean remove(K key) {
        assertOpened();
        boolean result = storage.remove(key);
        if (result) {
            proxyStorage.proxyDelete(key);
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(this, entryFactory.create(key, null), EventType.REMOVED)
            );
            eventsDispatcher.dispatch();
        }
        return result;
    }

    @Override
    public boolean remove(K key, V oldValue) {
        assertOpened();
        StorableEntry<K, V> value = storage.get(key);
        if (value == null) {
            return false;
        }
        if (value.getValue().equals(oldValue)) {
            remove(key);
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(this, value, EventType.REMOVED)
            );
            eventsDispatcher.dispatch();
            return true;
        }
        return false;
    }

    @Override
    public V getAndRemove(K key) {
        assertOpened();
        V value = get(key);
        if (value != null) {
            remove(key);
        }
        return value;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        assertOpened();
        StorableEntry<K, V> entry = storage.get(key);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.UPDATE);
        if (entry == null) {
            return false;
        }
        if (entry.equals(oldValue)) {
            StorableEntry<K,V> updatedEntry = entryFactory.create(key, newValue);
            if(storage.update(updatedEntry)) {
                eventsDispatcher.storeEvent(
                        new StoredEntryEvent<>(this,entry, EventType.UPDATED)
                );
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean replace(K key, V value) {
        assertOpened();
        if (!storage.containsKey(key)) {
            return false;
        }
        StorableEntry<K,V> entry = entryFactory.create(key, value);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.CREATE);
        if(entry==null) { // rare case: already expired, not updating
            return false;
        }
        if(storage.update(entry)) {
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(this,entry, EventType.UPDATED)
            );
            return true;
        } else {
            return false;
        }
    }

    @Override
    public V getAndReplace(K key, V value) {
        assertOpened();
        V oldValue = get(key);
        if(oldValue==null) {
            return null;
        }
        StorableEntry<K,V> entry = entryFactory.create(key, value);
        entry = policyExecutor.execute(entry, PolicyExecutor.Action.CREATE);
        if(entry==null) { // rare case: already expired, not updating
            return oldValue;
        }
        if(storage.update(entry)) {
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(this,entry, EventType.UPDATED)
            );
        }
        return oldValue;
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        assertOpened();
        storage.removeAll(keys);
        proxyStorage.proxyDeleteAll(keys);
        for (K key : keys) {
            eventsDispatcher.storeEvent(
                    new StoredEntryEvent<>(
                        this, entryFactory.create(key, null), //TODO: is it necessary to provide value?
                        EventType.REMOVED
                    )
            );
        }
        eventsDispatcher.dispatch();
    }

    @Override
    public void removeAll() {
        assertOpened();
        storage.removeAll();
    }

    @Override
    public void clear() {
        removeAll();
    }

    @Override
    public <CL extends Configuration<K, V>> CL getConfiguration(Class<CL> clazz) {
        if (clazz == null) {
            return (CL) configuration;
        }
        if (!clazz.isInstance(configuration)) {
            throw new IllegalArgumentException(String.format(
                    "present configurator class (%s) not matched Cache configurator (%s)",
                    clazz.getCanonicalName(), configuration.getClass().getCanonicalName()
            ));
        }
        return (CL) configuration;
    }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws EntryProcessorException
    {
        assertOpened();
        V value = get(key);
        StorableMutableEntry<K,V> mutableEntry = new StorableMutableEntry<>(this,key,value);
        return entryProcessor.process(mutableEntry, arguments);
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(
            Set<? extends K> keys, final EntryProcessor<K, V, T> entryProcessor, final Object... arguments
    ) {
        assertOpened();
        Map<K,V> map = getAll(keys);
        Map<K, EntryProcessorResult<T>> results = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            final StorableMutableEntry<K,V> mutableEntry = new StorableMutableEntry<>(this,entry.getKey(),entry.getValue());
            EntryProcessorResult<T> result = new EntryProcessorResult<T>() {
                @Override
                public T get() throws EntryProcessorException {
                    return entryProcessor.process(mutableEntry, arguments);
                }
            };
            results.put(entry.getKey(), result);
        }
        return results;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public CacheManager getCacheManager() {
        return manager;
    }

    @Override
    public void close() {
        storage.close();
    }

    @Override
    public boolean isClosed() {
        return storage.isClosed();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return clazz.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + clazz + " is not a supported by this implementation");
    }

    @Override
    public void loadAll(
            final Set<? extends K> keys,
            final boolean replaceExistingValues,
            final CompletionListener completionListener
    ) {
        assertOpened();
        manager.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (replaceExistingValues) {
                        loadReplace(keys);
                    } else {
                        loadAdd(keys);
                    }
                } catch (Exception e) {
                    completionListener.onException(e);
                }
                completionListener.onCompletion();

            }
        });
    }

    private void loadReplace(final Set<? extends K> keys) throws Exception {
        Map<K,V> externalValues = proxyStorage.proxytLoadAll(keys, null);
        storage.removeAll(keys); // TODO: update existed instead of deleting all (?)
        Collection<StorableEntry<K,V>> loadedEnties = new ArrayList<>(externalValues.size());
        for (Map.Entry<K, V> mapEntry : externalValues.entrySet()) {
            StorableEntry<K,V> entry = entryFactory.create(mapEntry.getKey(), mapEntry.getValue());
            loadedEnties.add(entry);
        }
        storage.putAll(loadedEnties);
    }

    private void loadAdd(final Set<? extends K> keys) throws Exception {
        Collection<StorableEntry<K,V>> cachedEntries = storage.getAll(keys);
        Map<K,V> loadedEntriesMap = proxyStorage.proxytLoadAll(keys, null);
        for (StorableEntry<K, V> entry : cachedEntries) {
            loadedEntriesMap.remove(entry.getKey());
        }
        storage.putAll(cachedEntries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        if (cacheEntryListenerConfiguration == null) {
            throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
        }
        configuration.addCacheEntryListenerConfiguration(cacheEntryListenerConfiguration);
        eventsDispatcher.register(cacheEntryListenerConfiguration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        if (cacheEntryListenerConfiguration == null) {
            throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
        }

        configuration.removeCacheEntryListenerConfiguration(cacheEntryListenerConfiguration);
        eventsDispatcher.unregister(cacheEntryListenerConfiguration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        assertOpened();
        Collection<StorableEntry<K, V>> values = storage.getAll();
        List<Entry<K, V>> entries = new ArrayList<>(values.size());
        for (StorableEntry<K, V> entry : values) {
            entries.add(entry);
        }
        return entries.iterator();
    }

    private void assertOpened() {
        if (isClosed()) {
            throw new IllegalStateException("CacheManager is closed");
        }
    }
}
