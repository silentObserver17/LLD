package LLDSolutions.LRUCache;

public class ThreadSafeLRUCache<K,V> {
    private final LRUCache<K, V> cache;
    private final Object lock = new Object();

    public ThreadSafeLRUCache(int capacity) {
        this.cache = new  LRUCache<>(capacity);
    }

    public V get(K key){
        synchronized(lock){
            return cache.get(key);
        }
    }

    public void put(K key, V value){
        synchronized(lock){
            cache.put(key, value);
        }
    }

    public int  size(){
        synchronized(lock){
            return cache.size();
        }
    }

    public void printCache(){
        synchronized(lock){
            cache.printCache();
        }
    }
}
