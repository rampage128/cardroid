package de.jlab.cardroid.rules.properties;

public class Property<T> {
    private T value;
    private String key;

    public Property(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public String getKey() {
        return this.key;
    }

}
