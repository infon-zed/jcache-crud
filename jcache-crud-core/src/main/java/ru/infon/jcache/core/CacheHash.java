package ru.infon.jcache.core;

/**
 * 19.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class CacheHash {

    private final String name;
    private final Class<?> keyType;
    private final Class<?> valueType;

    public CacheHash(String name, Class<?> keyType, Class<?> valueType) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public Class<?> getKeyType() {
        return keyType;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheHash that = (CacheHash) o;

        if (!name.equals(that.name)) return false;
        if (!keyType.equals(that.keyType)) return false;
        return valueType.equals(that.valueType);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + keyType.hashCode();
        result = 31 * result + valueType.hashCode();
        return result;
    }
}
