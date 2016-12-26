package ru.infon.jcache.mongo.basic;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import ru.infon.jcache.mongo.JustPojo;
import ru.infon.jcache.mongo.MongoTests;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class PlainMongoTests extends MongoTests {

    MongoClient mongoClient;
    Morphia morphia;
    Datastore datastore;
    MongoDatabase database;

    @Before
    public void connect() throws URISyntaxException {
        URI uri = connectionURI;
        System.out.println("host"+uri.getHost());
        System.out.println("uri string:"+uri.toString());
        MongoClientURI mongoClientURI = new MongoClientURI(uri.toString());
        System.out.println("database by mongo URI>>> "+mongoClientURI.getDatabase());
        mongoClient = new MongoClient(mongoClientURI);

        String selectedDatabaseName = mongoClientURI.getDatabase();

        morphia = new Morphia();
        database = mongoClient.getDatabase(selectedDatabaseName);
        datastore = morphia.createDatastore(mongoClient, selectedDatabaseName);
    }


    @Test
    public void testMongoWithMorphia() throws URISyntaxException {

        SimpleMongoObject testObject = new SimpleMongoObject("PlainMongoTests");
        UUID id = testObject.getCacheId();
        System.out.println("id: "+id);
        Key objectKey = datastore.save(testObject);
        System.out.println("object key: "+objectKey);

    }

    @Test
    public void testMongoWithMorphiaComplex() throws URISyntaxException {

        JustPojo pojo = new JustPojo("pojo value", "just pojo");
        UUID id = UUID.randomUUID();

        ComplexMongoObject<JustPojo> testObject = new ComplexMongoObject<>(pojo, id.toString());
        System.out.println("id: "+id);

        Key objectKey = datastore.save(testObject);
        System.out.println("object key: "+objectKey);

        Query<ComplexMongoObject> query = datastore.find(ComplexMongoObject.class, ComplexMongoObject.KEY_FIELD, id.toString());
        ComplexMongoObject loaded = query.get();

        System.out.println("loaded object plain: "+loaded);

    }

    @Test
    public void testMongoWithMorphiaUnbind() throws URISyntaxException {
        String collection  = "custom_cache_collection_name_2";
        UUID id = UUID.randomUUID();
        String idString = id.toString();
        JustPojo pojo = new JustPojo("some value", "just pojo "+System.currentTimeMillis());

        System.out.println("id: "+idString);
        // pack to CacheObject

        CacheMongoObject<String,JustPojo> cacheObject = new CacheMongoObject<>(
              idString, pojo
        );

        // ------------- saving -------------

        DBObject valueObject = morphia.toDBObject(cacheObject.getValue());

        BasicDBObject innerObject = new BasicDBObject();
        innerObject.put(CacheMongoObject.FIELD_KEY, idString);
        innerObject.put(CacheMongoObject.FIELD_VALUE,valueObject);

        //DBObject dbObject = morphia.toDBObject(cacheObject);
        System.out.println("bson object: "+innerObject);
        database.getCollection(collection, DBObject.class).insertOne(innerObject);
        cacheObject.setId((ObjectId)innerObject.get("_id"));
        System.out.println("bson object after save: "+innerObject);

        // ------------- loading -------------

        BasicDBObject findObject = new BasicDBObject(CacheMongoObject.FIELD_KEY, idString);
        FindIterable<DBObject> loadedList = database.getCollection(collection, DBObject.class).find(findObject);
        Class<JustPojo> clazz = JustPojo.class;
        DBObject valueDBObject = (DBObject) loadedList.first().get(CacheMongoObject.FIELD_VALUE);
        JustPojo pojoLoaded = morphia.fromDBObject(datastore, clazz, valueDBObject);

        CacheMongoObject<String, JustPojo> loaded = new CacheMongoObject<>();
        loaded.setKey((String)loadedList.first().get(CacheMongoObject.FIELD_KEY));
        loaded.setValue(pojoLoaded);
        loaded.setId((ObjectId)loadedList.first().get("_id"));

        // ------------- validate -------------

        System.out.println(pojoLoaded);
        Assert.assertEquals(cacheObject.getValue().value, loaded.getValue().value);
        Assert.assertEquals(cacheObject.getValue().description, loaded.getValue().description);
        Assert.assertTrue(cacheObject.equals(loaded));
    }

    @Test
    public void testMongoWithMorphiaKeyValue() throws URISyntaxException {
        String collection  = "custom_cache_collection_name";
        UUID id = UUID.randomUUID();
        JustPojo pojo = new JustPojo(id.toString(), "just pojo");

        System.out.println("id: "+id);

        DBObject dbObject = morphia.toDBObject(pojo);

        System.out.println("bson object: "+dbObject);

        database.getCollection(collection, DBObject.class).insertOne(dbObject);

        System.out.println("bson object after save: "+dbObject);

        BasicDBObject findObject = new BasicDBObject("value", id.toString());
        FindIterable<DBObject> loadedList = database.getCollection(collection, DBObject.class).find(findObject);

        JustPojo loaded = morphia.fromDBObject(datastore, JustPojo.class, loadedList.first());

        System.out.println(loaded);
    }

}
