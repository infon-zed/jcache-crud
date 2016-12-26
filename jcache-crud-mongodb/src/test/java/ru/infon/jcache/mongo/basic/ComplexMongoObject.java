package ru.infon.jcache.mongo.basic;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

/**
 * 20.10.2016
 * @author kostapc
 * 2016 Infon
 */
@Entity(ComplexMongoObject.COLLECTION)
public class ComplexMongoObject<T> {

    public static final String COLLECTION = "MongoComplexObject_class";
    public static final String KEY_FIELD = "key";

    @Id
    private ObjectId id;

    private T value;

    @Indexed @Property(KEY_FIELD)
    private String key;

    public ComplexMongoObject() {
    }

    public ComplexMongoObject(T value, String key) {
        this.value = value;
        this.key = key;
    }

    public T getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public ObjectId getId() {
        return id;
    }
}
