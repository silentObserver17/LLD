package LLDSolutions.LRUCache;

public class LRUEvictionPolicy<K, V> {
    // sentinel nodes never hold real data.
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUEvictionPolicy() {
        head = new Node<>(null, null); // dummy Head
        tail = new Node<>(null, null); // dummy Tail
        head.next = tail;
        tail.prev = head;
    }

    public K evict() {
        // LRU node is just before TAIL
        Node<K, V> lruNode = tail.prev;
        if (lruNode == head) return null;
        removeNode(lruNode);
        return lruNode.key;
    }

    // ── Internal linked list operations ──────────────────────
    public void addToFront(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    public void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        // don't null out prev/next — node might be re-inserted
    }

    public void moveToFront(Node<K, V> node) {
        removeNode(node);
        addToFront(node);
    }

    public void printCache(){
        StringBuilder sb = new StringBuilder("MRU -> ");
        Node<K, V> curr = head.next;

        while (curr != tail){
            sb.append("[").append(curr.key).append(":").append(curr.value).append("] ");
            curr = curr.next;
        }
        sb.append("<- LRU");
        System.out.println(sb.toString());
    }
}
