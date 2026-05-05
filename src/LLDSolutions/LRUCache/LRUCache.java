package LLDSolutions.LRUCache;

import java.util.HashMap;
import java.util.Map;

// CORE CLASS NOT THREAD SAFE
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K,Node<K,V>> map;
    private final LRUEvictionPolicy<K, V> policy;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + capacity);
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.policy = new LRUEvictionPolicy<>();
    }

    // --- PUBLIC API ---------------------------------------
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null)  return null;

        policy.moveToFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> existing =  map.get(key);

        if (existing != null) {
            // Key exists - update value and move to the front
            existing.value = value;
            policy.moveToFront(existing);
            return;
        }

        // New Key - create node and insert
        Node<K, V> node = new Node<>(key, value);
        map.put(key, node);
        policy.addToFront(node);

        // Over capacity — evict LRU
        if(map.size() > capacity) {
            K evictedKey = policy.evict();
            if(evictedKey != null) {
                map.remove(evictedKey);
            }
        }
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(K key){
        return map.containsKey(key);
    }

    // Debug Helper ----------------------------------------
    public void printCache(){
        policy.printCache();
    }
}
