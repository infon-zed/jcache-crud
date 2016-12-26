package ru.infon.jcache.mongo;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.util.UUID;

/**
 * 13.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class BasicImplementationTest {

    @Ignore("for basic RI implementation")
    @Test
    public void testSaveAndLoad() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager();
        MutableConfiguration<String, CacheEntity> config = new MutableConfiguration<>();
        Cache<String, CacheEntity> cache  = manager.createCache("base", config);

        final String key = UUID.randomUUID().toString();
        CacheEntity entity = new CacheEntity(13,"number 13");

        cache.put(key, entity);

        CacheEntity loaded = cache.get(key);

        System.out.println(loaded);
        Assert.assertEquals(entity, loaded);
    }

}
