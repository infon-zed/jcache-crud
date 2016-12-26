package ru.infon.jcache.core;

import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * 18.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class ProxyStorage<K,V> {
    private final CacheLoader<K, V> loader;
    private final CacheWriter<K, V> writer;

    public ProxyStorage(CacheLoader<K, V> loader, CacheWriter<? super K, ? super V> writer) {
        if(loader==null) {
            this.loader = new LoaderMock();
        } else {
            this.loader = loader;
        }
        if(writer==null) {
            this.writer = new WriterMock();
        } else {
            this.writer = (CacheWriter<K, V>) writer;
        }
    }

    public ProxyStorage(CompleteConfiguration<K,V> configuration) {
        this(
            configuration.isReadThrough()?configuration.getCacheLoaderFactory().create():null,
            configuration.isWriteThrough()?configuration.getCacheWriterFactory().create():null
        );
    }

    V proxyLoad(K key,V cached) {
        if(cached==null) {
            return loader.load(key);
        } else {
            return cached;
        }
    }

    Map<K, V> proxytLoadAll(Iterable<? extends K> keys, Map<K,V> cachedValues) {
        if(cachedValues==null || cachedValues.size()==0) {
            return loader.loadAll(keys);
        }
        return cachedValues;
    }

    void proxyWrite(Cache.Entry<K, V> entry) {
        writer.write(entry);
    }

    void proxyWriteAll(Collection<StorableEntry<K, V>> entries) {
        Collection<Cache.Entry<? extends K,? extends V>> proxyCollection = new ArrayList<>(entries.size());
        for (Cache.Entry<K, V> entry : entries) {
            proxyCollection.add(entry);
        }
        writer.writeAll(proxyCollection);
    }

    void proxyDelete(K key) {
        writer.delete(key);
    }

    void proxyDeleteAll(Collection<? extends K> keys) {
        writer.deleteAll(keys);
    }

    private class LoaderMock implements CacheLoader<K,V> {

        @Override
        public V load(K key) throws CacheLoaderException {
            return null;
        }

        @Override
        public Map<K, V> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
            return null;
        }
    }

    private class WriterMock implements CacheWriter<K,V> {

        @Override
        public void write(Cache.Entry<? extends K, ? extends V> entry) throws CacheWriterException {

        }

        @Override
        public void writeAll(Collection<Cache.Entry<? extends K, ? extends V>> entries) throws CacheWriterException {

        }

        @Override
        public void delete(Object key) throws CacheWriterException {

        }

        @Override
        public void deleteAll(Collection<?> keys) throws CacheWriterException {

        }
    }

}
