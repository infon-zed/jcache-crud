package ru.infon.jcache.mongo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 24.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoStorageCacheTest {

    private static final String COLLECTION = "jcache_MongoStorageCacheTest";
    private static final int expire_seconds = 1;

    private Cache<String, JustPojo> cache;

    @Before
    public void before() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager(
                MongoTests.connectionURI, null
        );
        MutableConfiguration<String, JustPojo> config = new MutableConfiguration<>();
        config.setTypes(String.class, JustPojo.class);
        config.setExpiryPolicyFactory(new Factory<ExpiryPolicy>() {
            @Override
            public ExpiryPolicy create() {
                return new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS,expire_seconds));
            }
        });
        cache = manager.getCache(COLLECTION);
        if(cache==null) {
            cache = manager.createCache(COLLECTION, config);
        }
        deleteAll();
    }

    public JustPojo generatePojo() {
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();
        JustPojo pojo = new JustPojo(key, "saveAndGetTest");
        return pojo;
    }

    public void deleteAll() {
        cache.removeAll();
        System.out.println("remvoed all entries");
    }

    public Collection<JustPojo> getAll() {
        List<JustPojo> loaded = new LinkedList<>();
        for (Cache.Entry<String, JustPojo> entry : cache) {
            loaded.add(entry.getValue());
        }
        return loaded;
    }

    @Test
    public void testPutAndGetFromCache() {
        JustPojo pojo = generatePojo();
        String key = pojo.value;
        cache.put(key, pojo);

        JustPojo loaded = cache.get(key);

        Assert.assertEquals(pojo,loaded);
    }

    @Test
    public void testGetByKeysAndDelete() {
        final int count = 10;
        Set<String> keys = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            keys.add(pojo.value);
            cache.put(pojo.value, pojo);
        }
        Map<String, JustPojo> saved = cache.getAll(keys);
        Assert.assertEquals(count, saved.size());
        cache.removeAll(keys);

        Collection<JustPojo> loaded = getAll();

        Assert.assertEquals(0,loaded.size());
    }

    @Test
    public void testGetAndDeleteAll() {
        final int count = 10;
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            cache.put(pojo.value, pojo);
        }


        Collection<JustPojo> entriesAll = getAll();

        Assert.assertEquals(count,entriesAll.size());
        cache.clear();

        Collection<JustPojo> entriesEmpty = getAll();
        Assert.assertEquals(0,entriesEmpty.size());
    }

    @Test
    public void tesPutAll () {
        final int count = 10;
        Set<String> keys = new HashSet<>(count);
        Map<String, JustPojo> objects = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            keys.add(pojo.value);
            objects.put(pojo.value, pojo);
        }
        cache.putAll(objects);

        Collection<JustPojo> entriesAll = getAll();
        Assert.assertEquals(count,entriesAll.size());

        Map<String, JustPojo> saved = cache.getAll(keys);
        Assert.assertEquals(count, saved.size());
    }

    @Test
    public void testContainsAndDelete() {
        JustPojo pojo = generatePojo();
        cache.put(pojo.value,pojo);
        Assert.assertTrue(
                cache.containsKey(pojo.value)
        );
        cache.remove(pojo.value);
        Assert.assertFalse(
                cache.containsKey(pojo.value)
        );
    }

    @Test
    public void testUpdates() {
        final int count = 10;
        Set<String> keys = new HashSet<>(count);
        Map<String, JustPojo> objects = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            keys.add(pojo.value);
            objects.put(pojo.value, pojo);
        }
        cache.putAll(objects);
        System.out.println("all added");
        // just update
        for (String key : keys) {
            JustPojo pojo = new JustPojo(key, "UPDATED "+key);
            cache.replace(key, pojo);
        }
        System.out.println("all updated");

        for (Cache.Entry<String, JustPojo> entry : cache) {
            Assert.assertEquals(
                "UPDATED "+entry.getKey(),
                entry.getValue().description
            );
        }
        System.out.println("all done");
    }

    @Test
    public void testPutWithExistsItems() {
        final int count = 10;
        Queue<String> keys = new LinkedList<>();
        Map<String, JustPojo> objects = new HashMap<>(count);
        for (int i = 0; i < count/2; i++) {
            JustPojo pojo = new JustPojo(
                    UUID.randomUUID().toString(),
                    "initial value"
            );
            keys.add(pojo.value);
            objects.put(pojo.value, pojo);
        }
        cache.putAll(objects);
        System.out.println("all added");

        final String prefix = "added in second loop ";

        Map<String, JustPojo> objectsMixed = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String key = keys.poll();
            if(key==null) {
                key = UUID.randomUUID().toString();
            }
            JustPojo pojo = new JustPojo(key,prefix+key);
            objectsMixed.put(pojo.value, pojo);
        }
        Assert.assertEquals(0,keys.size());
        cache.putAll(objectsMixed);
        System.out.println("all added with replace");

        int allCount = 0;
        for (Cache.Entry<String, JustPojo> entry : cache) {
            allCount ++;
            Assert.assertEquals(
                    prefix+entry.getKey(),
                    entry.getValue().description
            );
        }
        Assert.assertEquals(count, allCount);

    }

}
