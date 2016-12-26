package ru.infon.jcache.mongo;

import java.io.Serializable;

/**
 * 13.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class CacheEntity implements Serializable {
    private int number;
    private String value;

    public CacheEntity(int number, String value) {
        this.number = number;
        this.value = value;
    }

    public CacheEntity() {
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CacheEntity{" +
                "number=" + number +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheEntity that = (CacheEntity) o;

        if (number != that.number) return false;
        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
