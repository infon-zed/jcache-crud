/**
 * Copyright 2011-2013 Terracotta, Inc.
 * Copyright 2011-2013 Oracle America Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ru.infon.jcache.core.management;

import ru.infon.jcache.core.StoredCacheMXBean;

import javax.cache.CacheException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Set;


/**
 * A convenience class for registering CacheStatisticsMBeans with an MBeanServer.
 *
 * @since 1.0
 */
public final class MBeanServerRegistrationUtility {

    //ensure everything gets put in one MBeanServer
    private static MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();


    private MBeanServerRegistrationUtility() {
        //prevent construction
    }


    /**
     * Utility method for registering CacheStatistics with the MBeanServer
     *
     * @param cache the cache to register
     */
    public static void registerCacheObject(StoredCacheMXBean cache) {
        //these can change during runtime, so always look it up
        ObjectName registeredObjectName = calculateObjectName(cache);

        try {
            if (!isRegistered(cache)) {
                mBeanServer.registerMBean(cache, registeredObjectName);
            }
        } catch (Exception e) {
            throw new CacheException(String.format(
                    "Error registering cache MXBeans for CacheManager %s. Error was %s",
                    registeredObjectName, e.getMessage()
            ), e);
        }
    }


    /**
     * Checks whether an ObjectName is already registered.
     *
     * @throws CacheException - all exceptions are wrapped in CacheException
     */
    private static boolean isRegistered(StoredCacheMXBean cache) {

        Set<ObjectName> registeredObjectNames;

        ObjectName objectName = calculateObjectName(cache);
        registeredObjectNames = mBeanServer.queryNames(objectName, null);

        return !registeredObjectNames.isEmpty();
    }


    /**
     * Removes registered CacheStatistics for a Cache
     *
     * @throws CacheException - all exceptions are wrapped in CacheException
     */
    public static void unregisterCacheObject(StoredCacheMXBean cache) {

        Set<ObjectName> registeredObjectNames;

        ObjectName objectName = calculateObjectName(cache);
        registeredObjectNames = mBeanServer.queryNames(objectName, null);

        //should just be one
        for (ObjectName registeredObjectName : registeredObjectNames) {
            try {
                mBeanServer.unregisterMBean(registeredObjectName);
            } catch (Exception e) {
                throw new CacheException("Error unregistering object instance "
                        + registeredObjectName + " . Error was " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates an object name using the scheme
     * "javax.cache:type=Cache&lt;Statistics|Configuration&gt;,CacheManager=&lt;cacheManagerName&gt;,name=&lt;cacheName&gt;"
     */
    private static ObjectName calculateObjectName(StoredCacheMXBean cache) {
        String cacheManagerName = mbeanSafe(cache.getCacheManager().getClass().getName());
        String cacheName = mbeanSafe(cache.getName());

        try {
            return new ObjectName("javax.cache:type=Cache,CacheManager=" + cacheManagerName + ",Cache=" + cacheName);
        } catch (MalformedObjectNameException e) {
            throw new CacheException("Illegal ObjectName for Management Bean. " +
                    "CacheManager=[" + cacheManagerName + "], Cache=[" + cacheName + "]", e);
        }
    }


    /**
     * Filter out invalid ObjectName characters from string.
     *
     * @param string input string
     * @return A valid JMX ObjectName attribute value.
     */
    private static String mbeanSafe(String string) {
        return string == null ? "" : string.replaceAll(",|:|=|\n", ".");
    }

}

