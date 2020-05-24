package st.redline;

import java.util.*;

public class OrderedMap<K, V> implements Map<K, V> {
    private List<Entry<K,V>> data = new ArrayList<>();

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        for (Entry<K, V> datum : data) {
            if(datum.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Entry<K, V> datum : data) {
            if(datum.value.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        for (Entry<K, V> datum : data) {
            if(datum.key.equals(key)) {
                return datum.value;
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        for (Entry<K, V> datum : data) {
            if(datum.key.equals(key)) {
                datum.value = value;
                return value;
            }
        }
        data.add(new Entry<>(key, value));
        return value;
    }

    @Override
    public V remove(Object key) {
        final int size = data.size();
        for (int i=0; i<size; i++) {
            Entry<K, V> datum = data.get(i);
            if(datum.key.equals(key)) {
                final V val = datum.value;
                data.remove(i);
                return val;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        data = new ArrayList<>();
    }

    @Override
    public Set<K> keySet() {
        final Set<K> keys = new HashSet<>();
        for (Entry<K, V> datum : data) {
            keys.add(datum.key);
        }
        return keys;
    }

    @Override
    public Collection<V> values() {
        final List<V> values = new ArrayList<>();
        for (Entry<K, V> datum : data) {
            values.add(datum.value);
        }
        return values;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new HashSet<>(data);
    }

    public K getKey(int index) {
        return data.get(index).key;
    }

    public V getValue(int index) {
        return data.get(index).value;
    }

    public static class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return value;
        }
    }
}
