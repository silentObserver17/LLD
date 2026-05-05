package LLDSolutions.LRUCache;

public class Main {
    public static void main(String[] args) {
        System.out.println("═══ Basic Operations ═══");

        LRUCache<Integer, String> cache = new LRUCache<>(3);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.printCache();
        // MRU -> [3:C] [2:B] [1:A] <- LRU

        System.out.println("get(1): " + cache.get(1)); // A — moves 1 to front
        cache.printCache();
        // MRU -> [1:A] [3:C] [2:B] <- LRU

        cache.put(4, "D"); // over capacity — evict LRU (2:B)
        cache.printCache();
        // MRU → [4:D] [1:A] [3:C] ← LRU

        System.out.println("get(2): " + cache.get(2)); // null — evicted

        System.out.println("\n═══ Update Existing Key ═══");
        cache.put(1, "A_updated"); // update existing
        cache.printCache();

        System.out.println("\n═══ Generic Types ═══");
        LRUCache<String, Integer> wordCache = new LRUCache<>(2);
        wordCache.put("hello", 1);
        wordCache.put("world", 2);
        wordCache.printCache();
        // MRU → [world:2] [hello:1] ← LRU

        wordCache.get("hello"); // move hello to front
        wordCache.put("java", 3); // evict world
        wordCache.printCache();
        // MRU → [java:3] [hello:1] ← LRU

        System.out.println("\n═══ Thread Safety Test ═══");
        ThreadSafeLRUCache<Integer, Integer> safeCache = new ThreadSafeLRUCache<>(100);

        // Spin up 10 threads hammering the cache simultaneously
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    safeCache.put(threadId * 100 + j, threadId * 100 + j);
                    safeCache.get(threadId * 100 + j);
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println("Thread-safe cache size: " + safeCache.size());
        // Always ≤ 100 — no corruption despite concurrent access
    }
}
