package com.github.bryan.logback.pulsar;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProducerRecord<K,V> {
    private K key;
    private V value;

    private Map<String,String> properties= new HashMap<>();

    public ProducerRecord(K key, V value, Map<String,String> properties) {
        this.key = key;
        this.value = value;
        if(properties.size() > 0){
            this.properties.putAll(properties);
        }
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProducerRecord<?, ?> that = (ProducerRecord<?, ?>) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
    @Override
    public String toString() {
        return "ProducerRecord{" +
                "key=" + key +
                ", value=" + value +
                ", properties=" + properties +
                '}';
    }
}
