package ru.infon.jcache.mongo;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 26.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class JcacheImplementationTest {

    private static final String COLLECTION = "jcache_JcacheImplementationTest";
    private Cache<String, JustPojo> cache;

    public void prepare() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager(
                MongoTests.connectionURI, null
        );
        MutableConfiguration<String, JustPojo> config = new MutableConfiguration<>();
        config.setTypes(String.class, JustPojo.class);
        config.setExpiryPolicyFactory(new Factory<ExpiryPolicy>() {
            @Override
            public ExpiryPolicy create() {
                return new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS,2));
            }
        });
        cache = manager.getCache(COLLECTION);
        if(cache==null) {
            cache = manager.createCache(COLLECTION, config);
        }
        deleteAll();
    }

    public void deleteAll() {
        cache.removeAll();
        System.out.println("remvoed all entries");
    }

    public JustPojo generatePojo() {
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();
        JustPojo pojo = new JustPojo(key, "saveAndGetTest");
        return pojo;
    }

    public void testExpirity() {
        JustPojo pojo = generatePojo();
        cache.put(pojo.value, pojo);
    }


}
