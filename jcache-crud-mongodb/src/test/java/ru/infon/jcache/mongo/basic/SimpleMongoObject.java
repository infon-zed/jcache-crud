package ru.infon.jcache.mongo.basic;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.Date;
import java.util.UUID;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */

@Entity(SimpleMongoObject.COLLECTION)
public class SimpleMongoObject {

    public static final String COLLECTION = "TestCachedObject_class";

    @Id
    private ObjectId objectId;

    @Property("cache_id")
    private String cacheId;

    private Date lastAccessDate;

    private String value;

    private UUID uuid;

    @Deprecated
    public SimpleMongoObject() {
        // constructor for loading by Morphia driver
    }

    public SimpleMongoObject(String value) {
        this.uuid = UUID.randomUUID();
        this.cacheId = this.uuid.toString();
        this.value = value;
        this.lastAccessDate = new Date();
    }

    public UUID getCacheId() {
        return uuid;
    }

    public Date getLastAccessDate() {
        return lastAccessDate;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SimpleMongoObject{" +
                "objectId=" + objectId +
                ", cacheId='" + cacheId + '\'' +
                ", lastAccessDate=" + lastAccessDate +
                ", value='" + value + '\'' +
                ", uuid=" + uuid +
                '}';
    }
}
