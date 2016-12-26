package ru.infon.jcache.mongo;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.*;
import javax.cache.spi.CachingProvider;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 26.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class ExpirityTest {

    private static final int expire_seconds = 1;
    static CacheManager manager;

    @BeforeClass
    public static void prepare() {
        CachingProvider provider = Caching.getCachingProvider();
        manager = provider.getCacheManager(
                MongoTests.connectionURI, null
        );
    }

    public <K,V> Cache<K,V> getCache(Class<K> keyType, Class<V> valueType, final ExpiryPolicy policy) {

        MutableConfiguration<K, V> config = new MutableConfiguration<>();
        config.setTypes(keyType, valueType);
        config.setExpiryPolicyFactory(new Factory<ExpiryPolicy>() {
            @Override
            public ExpiryPolicy create() {
                return policy;
            }
        });
        String collectionName = policy.getClass().getName();
        Cache<K,V> cache = manager.getCache(collectionName);
        if(cache==null) {
            cache = manager.createCache(collectionName, config);
        }
        cache.removeAll();
        return cache;
    }

    @Test
    public void testAccessExpire() throws InterruptedException {
        Cache<String, JustPojo> cache = getCache(
                String.class, JustPojo.class,
                new AccessedExpiryPolicy(new Duration(TimeUnit.SECONDS,expire_seconds))
        );
        String key = UUID.randomUUID().toString();
        JustPojo pojo = new JustPojo(key, "testAccessExpire()");
        cache.put(key, pojo);
        int count = (expire_seconds*2*1000)/100;
        for (int i = 0; i < count; i++) {
            long sleepTime = 100;
            System.out.println(String.format(
                    "%s/%S) sleeping %s mills",
                    i, count, sleepTime
            ));
            Thread.sleep(sleepTime);
            JustPojo loaded = cache.get(key);
            Assert.assertEquals(pojo, loaded);
        }
        Thread.sleep(expire_seconds * 1000);
        JustPojo loaded = cache.get(key);
        Assert.assertNull(loaded);
    }

    @Test
    public void testCreatedExpire() throws InterruptedException {
        Cache<String, JustPojo> cache = getCache(
                String.class, JustPojo.class,
                new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS,expire_seconds))
        );
        String key = UUID.randomUUID().toString();
        JustPojo pojo = new JustPojo(key, "testCreatedExpire()");
        cache.put(key, pojo);
        Thread.sleep((expire_seconds+1)*1000);
        JustPojo loaded = cache.get(key);
        Assert.assertNull(loaded);
    }

    @Test
    public void testModifyExpire() throws InterruptedException {
        Cache<String, JustPojo> cache = getCache(
                String.class, JustPojo.class,
                // expires after modification or after creation
                new ModifiedExpiryPolicy(new Duration(TimeUnit.SECONDS,expire_seconds))
        );
        String key = UUID.randomUUID().toString();
        JustPojo pojo = new JustPojo(key, "testModifyExpire()");
        cache.put(pojo.value, pojo);
        Thread.sleep(500);
        JustPojo loaded;
        loaded = cache.get(key);
        Assert.assertEquals(pojo,loaded);
        cache.replace(key, loaded, new JustPojo("new value","new description"));
        Thread.sleep((expire_seconds*1000)/2-100);
        loaded = cache.get(key);
        Assert.assertEquals(pojo,loaded);
        Thread.sleep((expire_seconds*1000)/2+100);
        loaded = cache.get(key);
        Assert.assertNull(loaded);
    }


}
