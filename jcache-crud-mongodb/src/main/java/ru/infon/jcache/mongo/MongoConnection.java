package ru.infon.jcache.mongo;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.beanutils.ConvertUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class MongoConnection {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");

    private static final MongoClientOptions defaultOptions = MongoClientOptions.builder().build();
    private static final Map<String, Method> optionsBuilderMap;

    static {
        optionsBuilderMap = new HashMap<>();
        for (Method method : MongoClientOptions.Builder.class.getMethods()) {
            if (method.getParameterTypes().length!=1){
                continue;
            }
            int access = method.getModifiers();
            if(!Modifier.isPublic(access) || Modifier.isStatic(access)) {
                continue;
            }

            String optionName = method.getName();
            Class paramType = method.getParameterTypes()[0];
            if(
                    !paramType.equals(Boolean.class) &&
                            !paramType.equals(Boolean.TYPE)  &&
                            !paramType.equals(Integer.class) &&
                            !paramType.equals(Integer.TYPE)  &&
                            !paramType.equals(String.class)
                    ) {
                continue;
            }
            String prefix =
                    (
                            paramType.equals(Boolean.class) ||
                            paramType.equals(Boolean.TYPE)
                    )?
                            "is":"get";
            String getterName = prefix+optionName.substring(0,1).toUpperCase()+optionName.substring(1);

            Object defaultValue;
            try {
                Method getter;
                getter = MongoClientOptions.class.getDeclaredMethod(getterName);
                defaultValue = getter.invoke(defaultOptions);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                LOGGER.warning(String.format(
                        "reflection error while checking: %s; with getter: %s",
                        optionName, getterName
                ));
                continue;
            }

            optionsBuilderMap.put(optionName,method);

            String infoMessage = String.format(
                    "MongoOptions param \"%s\" = \"%s\" (default, getter: %s)",
                    optionName,  defaultValue, getterName
            );

            LOGGER.info(infoMessage);
        }
    }

    private final MongoDatabase mongoDB;
    private final Datastore datastore;
    private final Morphia morphia;
    private final MongoClient client;

    public MongoConnection(MongoURIWrapper mongoClientURI, Properties properties) {

        if(mongoClientURI==null) {
            throw new NullPointerException("mongoClientURI cannot be null");
        }
        if(properties==null) {
            throw new NullPointerException("properties cannot be null");
        }

        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();

        String dbName = mongoClientURI.getDatabase();
        String username = mongoClientURI.getUsername();
        char[] password = mongoClientURI.getPassword();
        MongoClientOptions options = mongoClientURI.getOptions();

        List<ServerAddress> uriAdresses = new ArrayList<>();
        List<ServerAddress> propertiesAdresses = new ArrayList<>();
        for (String hostName : mongoClientURI.getHosts()) {
            uriAdresses.add(new ServerAddress(hostName));
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getKey().toString();
            if (key.startsWith("server")) {
                propertiesAdresses.add(new ServerAddress(value));
            } else if (key.equals("database")) {
                dbName = value;
            } else if (key.startsWith("username")) {
                username = value;
            } else if (key.startsWith("password")) {
                password = value.toCharArray();
            } else {
                options = null;
                try {
                    LOGGER.fine(MessageFormat.format("Set \"{0}\" value {1}", key, value));
                    Method method = optionsBuilderMap.get(key);
                    if(method==null) {
                        LOGGER.warning(String.format(
                                "MongoClientOptions parameter %s => %s not found in configuration class; skipping...",
                                key, value
                        ));
                        continue;
                    }
                    Object methodParam = ConvertUtils.convert(value, method.getParameterTypes()[0]);
                    method.invoke(optionsBuilder, methodParam);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        if (dbName == null || username == null) {
            throw new RuntimeException("Mandatory property \"database\" not found");
        }

        List<MongoCredential> credentials =  Collections.singletonList(
                MongoCredential.createCredential(
                        username, dbName, password
                )
        );

        if (options==null) {
            WriteConcern writeConcern = WriteConcern.W1;
            writeConcern.withJournal(true);
            writeConcern.withWTimeout(0, TimeUnit.MILLISECONDS);
            optionsBuilder.writeConcern(writeConcern);
            options = optionsBuilder.build();
        }


        client = new MongoClient(
                propertiesAdresses.size()>0?propertiesAdresses:uriAdresses,
                credentials, options
        );
        mongoDB = client.getDatabase(dbName);
        morphia = new Morphia();
        datastore = morphia.createDatastore(client, dbName);
    }

    /*===========================================[ CLASS METHODS ]==============*/

    public Datastore getDatastore() {
        return datastore;
    }

    public MongoDatabase getMongoDB() {
        return mongoDB;
    }

    public Morphia getMorphia() {
        return morphia;
    }

    public MongoClient getMongoClient() {
        return client;
    }

}
