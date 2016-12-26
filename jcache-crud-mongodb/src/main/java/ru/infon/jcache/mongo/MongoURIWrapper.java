package ru.infon.jcache.mongo;

import com.mongodb.MongoClientURI;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoURIWrapper extends MongoClientURI {

    private final URI originalURI;

    public MongoURIWrapper(URI originalURI) {
        super(originalURI.toString());
        this.originalURI = originalURI;
    }

    public MongoURIWrapper(MongoClientURI mongoURI) {
        super(mongoURI.getURI());
        this.originalURI = null;
    }

    public MongoURIWrapper(String mongoURI) {
        super(mongoURI);
        try {
            this.originalURI = new URI(mongoURI);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "malformed URI: "+mongoURI, e
            );
        }
    }

    public boolean isMongoURIUsed() {
        return originalURI==null;
    }
}
