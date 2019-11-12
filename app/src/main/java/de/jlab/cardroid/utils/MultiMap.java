package de.jlab.cardroid.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import androidx.annotation.NonNull;

public final class MultiMap<K extends Class<?>, V> {

    private HashMap<K, ArrayList<V>> entries = new HashMap<>();

    public void put(@NonNull K key, @NonNull V value) {
        ArrayList<V> bucket = this.getOrCreateBucket(key);
        if (!bucket.contains(value)) {
            bucket.add(value);
        }
    }

    public ArrayList<V> get(@NonNull K key) {
        ArrayList<V> bucket = this.entries.get(key);
        if (bucket != null) {
            return new ArrayList<>(bucket);
        }
        return new ArrayList<>();
    }

    public ArrayList<V> getAssignable(@NonNull K desiredType) {
        ArrayList<V> result = new ArrayList<>();
        for (K key : this.entries.keySet()) {
            if (key.isAssignableFrom(desiredType)) {
                ArrayList<V> localEntries = this.entries.get(key);
                if (localEntries != null) {
                    result.addAll(localEntries);
                }
            }
        }
        return result;
    }

    public Set<K> keySet() {
        return this.entries.keySet();
    }

    public void remove(@NonNull K key, @NonNull V value) {
        ArrayList<V> bucket = this.entries.get(key);
        if (bucket != null) {
            bucket.remove(value);
        }
    }

    public void clear() {
        this.entries.clear();
    }

    private ArrayList<V> getOrCreateBucket(@NonNull K key) {
        ArrayList<V> bucket = this.entries.get(key);
        if (bucket == null) {
            bucket = new ArrayList<>();
            this.entries.put(key, bucket);
        }
        return bucket;
    }

}
