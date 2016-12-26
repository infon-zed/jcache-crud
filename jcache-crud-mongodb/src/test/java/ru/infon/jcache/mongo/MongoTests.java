package ru.infon.jcache.mongo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * 17.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoTests {

    private static final String propertiesFile = "mongo.properties";
    protected static final URI connectionURI = connectionURI();

    private static URI connectionURI() {
        try {
            Properties properties = new Properties();
            properties.load(
                MongoTests.class.getClassLoader()
                        .getResourceAsStream(propertiesFile)
            );
            String mongoUrl = (String) properties.get("mongo.url");
            String mongodbDatabase = (String) properties.get("mongodb.database");
            String mongodbUser = (String) properties.get("mongodb.user");
            String mongodbPass = (String) properties.get("mongodb.pass");
            return new URI(String.format(
                    "mongodb://%s:%s@%s/%s",
                    mongodbUser, mongodbPass,
                    mongoUrl,mongodbDatabase
            ));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }


}
