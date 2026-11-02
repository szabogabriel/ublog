package info.gabrielszabo.ublog.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapResolver<K, V> implements Map<K, V> {

    private java.util.Map<K, V> mapPrimary;
    private java.util.Map<K, V> mapSecondary;

    public MapResolver() {

    }

    public MapResolver(java.util.Map<K, V> mapPrimary, java.util.Map<K, V> mapSecondary) {
        if (mapPrimary == null || mapSecondary == null) {
            throw new IllegalArgumentException("Both mapPrimary and mapSecondary must be non-null");
        }
        this.mapPrimary = mapPrimary;
        this.mapSecondary = mapSecondary;
    }

    public void setPrimaryMap(java.util.Map<K, V> mapPrimary) {
        if (mapPrimary == null) {
            throw new IllegalArgumentException("mapPrimary must be non-null");
        }
        this.mapPrimary = mapPrimary;
    }

    public void setSecondaryMap(java.util.Map<K, V> mapSecondary) {
        if (mapSecondary == null) {
            throw new IllegalArgumentException("mapSecondary must be non-null");
        }
        this.mapSecondary = mapSecondary;
    }

    @Override
    public int size() {
        return mapPrimary.size() + mapSecondary.size();
    }

    @Override
    public boolean isEmpty() {
        return mapPrimary.isEmpty() && mapSecondary.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mapPrimary.containsKey(key) || mapSecondary.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mapPrimary.containsValue(value) || mapSecondary.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V ret = mapPrimary.get(key);
        if (ret == null) {
            ret = mapSecondary.get(key);
        }
        return ret;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("Unimplemented method 'put'");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Unimplemented method 'putAll'");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new java.util.HashSet<>(mapPrimary.keySet());
        keys.addAll(mapSecondary.keySet());
        return keys;
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = new java.util.ArrayList<>(mapPrimary.values());
        values.addAll(mapSecondary.values());
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new java.util.HashSet<>(mapPrimary.entrySet());
        entries.addAll(mapSecondary.entrySet());
        return entries;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(mapPrimary);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MapResolver<?, ?> other = (MapResolver<?, ?>) obj;
        return java.util.Objects.equals(mapPrimary, other.mapPrimary);
    }
    
}

