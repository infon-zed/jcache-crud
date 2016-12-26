package ru.infon.jcache.mongo;

/**
 * 14.10.2016
 * @author kostapc
 * 2016 Infon
 */
public class JustPojo {

    public String value;
    public String description;

    public JustPojo() {
    }

    public JustPojo(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public String toString() {
        return "JustPojo{" +
                "value='" + value + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JustPojo)) return false;

        JustPojo pojo = (JustPojo) o;

        if (!value.equals(pojo.value)) return false;
        return description.equals(pojo.description);

    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }
}
