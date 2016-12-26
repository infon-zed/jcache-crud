package ru.infon.jcache.mongo.basic;

import ru.infon.jcache.mongo.MongoConnection;
import ru.infon.jcache.mongo.MongoTests;
import ru.infon.jcache.mongo.MongoURIWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;

import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;

/**
 * 17.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoDriverTest extends MongoTests {

    @Test
    public void testMongoConnection() throws URISyntaxException {
        MongoURIWrapper uriWrapper = new MongoURIWrapper(connectionURI);
        MongoConnection connection = new MongoConnection(uriWrapper, new Properties());

        SimpleMongoObject testObject = new SimpleMongoObject("PlainMongoTests");
        UUID id = testObject.getCacheId();
        System.out.println("id: "+id);
        Key objectKey = connection.getDatastore().save(testObject);
        System.out.println("object key: "+objectKey);

        Query<SimpleMongoObject> loadQuery = connection.getDatastore().find(
                SimpleMongoObject.class, "uuid",
                testObject.getCacheId()
        );

        SimpleMongoObject loadedObject = loadQuery.get();

        System.out.println("loaded object: "+loadedObject);
        Assert.assertEquals(testObject.getCacheId(), loadedObject.getCacheId());
        Assert.assertEquals(testObject.getValue(), loadedObject.getValue());
    }
}
