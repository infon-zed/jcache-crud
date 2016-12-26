package ru.infon.jcache.mongo;

import ru.infon.jcache.core.CacheStorage;
import ru.infon.jcache.core.CacheStorageSession;
import ru.infon.jcache.core.StorableEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.*;

/**
 * 24.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class BasicMongoStorageTest extends MongoTests {
    private static final String COLLECTION = "jcache_BasicMongoStorageTest";

    private CacheStorageSession<String, JustPojo> session;
    private Class<String> keyType = String.class;
    private Class<JustPojo> valueType = JustPojo.class;

    @Before
    public void prepare() throws URISyntaxException {
        Properties properties = new Properties();
        CacheStorage storage = new MongoCacheStorage(connectionURI, properties);
        session = storage.openSession(
                COLLECTION,
                keyType, valueType
        );
        deleteAll();
    }

    public void deleteAll() {
        Collection<StorableEntry<String, JustPojo>> entries = session.getAll();
        Set<String> keys = new HashSet<>(entries.size());
        System.out.println("deleting existed entries:");
        for (StorableEntry<String, JustPojo> entry : entries) {
            String key = entry.getKey();
            keys.add(key);
            System.out.println("key>> "+key);
        }
        session.removeAll(keys);
        System.out.println("remvoed "+keys.size()+" entries");
    }

    private StorableEntry<String, JustPojo> entry(JustPojo pojo) {
        return session.getEntryFactory().create(pojo.value, pojo);
    }

    public JustPojo generatePojo() {
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();
        JustPojo pojo = new JustPojo(key, "saveAndGetTest");
        return pojo;
    }

    @Test
    public void putAndGetTest() {
        JustPojo pojo = generatePojo();
        session.put(entry(pojo));
        StorableEntry<String, JustPojo> loaded = session.get(pojo.value);
        Assert.assertEquals(pojo, loaded.getValue());
    }

    @Test
    public void testGetByKeysAndDelete() {
        final int count = 10;
        Set<String> keys = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            keys.add(pojo.value);
            session.put(entry(pojo));
        }
        Collection<StorableEntry<String, JustPojo>> saved = session.getAll(keys);
        Assert.assertEquals(count, saved.size());
        session.removeAll(keys);
        Collection<StorableEntry<String, JustPojo>> entriesEmpty = session.getAll();
        Assert.assertEquals(0,entriesEmpty.size());
    }

    @Test
    public void testGetAndDeleteAll() {
        final int count = 10;
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            session.put(entry(pojo));
        }
        Collection<StorableEntry<String, JustPojo>> entriesAll = session.getAll();
        Assert.assertEquals(count,entriesAll.size());
        deleteAll();
        Collection<StorableEntry<String, JustPojo>> entriesEmpty = session.getAll();
        Assert.assertEquals(0,entriesEmpty.size());
    }

    @Test
    public void tesPutAll () {
        final int count = 10;
        Set<String> keys = new HashSet<>(count);
        List<StorableEntry<String, JustPojo>> objects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            JustPojo pojo = generatePojo();
            keys.add(pojo.value);
            objects.add(entry(pojo));
        }
        session.putAll(objects);
        Collection<StorableEntry<String, JustPojo>> entriesAll = session.getAll();
        Assert.assertEquals(count,entriesAll.size());
        Collection<StorableEntry<String, JustPojo>> saved = session.getAll(keys);
        Assert.assertEquals(count, saved.size());
    }

    @Test
    public void testContainsAndDelete() {
        JustPojo pojo = generatePojo();
        session.put(entry(pojo));
        Assert.assertTrue(
            session.containsKey(pojo.value)
        );
        session.remove(pojo.value);
        Assert.assertFalse(
                session.containsKey(pojo.value)
        );
    }
}