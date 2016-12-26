package ru.infon.jcache.mongo;

import com.mongodb.BasicDBObject;
import org.bson.*;
import org.bson.conversions.Bson;

import java.util.Set;

import static ru.infon.jcache.mongo.MongoCacheEntity.FIELD_KEY;


/**
 * 24.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoQueryHelper {

    static <K> BsonValue getKeyBson(K key) {
        BsonValue keyValue;
        if(key==null) {
            keyValue = new BsonNull();
        } else {
            if (key instanceof Number) {
                keyValue = new BsonInt64((Long) key);
            } else {
                keyValue = new BsonString(key.toString());
            }
        }
        return keyValue;
    }

    static <K> Bson createFindQuery(K key) {
        // TODO: use expireDate in search request
        return new BasicDBObject(FIELD_KEY, key);
    }

    static <K> Bson createFindQuery(Set<? extends K> keys) {
        BsonArray in = new BsonArray();
        for (K key : keys) {
            in.add(getKeyBson(key));
        }
        BasicDBObject inQuery = new BasicDBObject("$in", in);
        return new BasicDBObject(FIELD_KEY, inQuery);
    }
}
