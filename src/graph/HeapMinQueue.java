package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A min priority queue of distinct elements of type `KeyType` associated with (extrinsic) integer
 * priorities, implemented using a binary heap paired with a hash table.
 */
public class HeapMinQueue<KeyType> implements MinQueue<KeyType> {

    /**
     * Pairs an element `key` with its associated priority `priority`.
     */
    private record Entry<KeyType>(KeyType key, int priority) {
    }

    /**
     * Associates each element in the queue with its index in `heap`.  Satisfies
     * `heap.get(index.get(e)).key().equals(e)` if `e` is an element in the queue. Only maps
     * elements that are in the queue (`index.size() == heap.size()`).
     */
    private final Map<KeyType, Integer> index;

    /**
     * Sequence representing a min-heap of element-priority pairs.  Satisfies
     * `heap.get(i).priority() >= heap.get((i-1)/2).priority()` for all `i` in `[1..heap.size()]`.
     */
    private final ArrayList<Entry<KeyType>> heap;

    /**
     * Assert that our class invariant is satisfied.  Returns true if it is (or if assertions are
     * disabled).
     */
    private boolean checkInvariant() {
        for (int i = 1; i < heap.size(); ++i) {
            int p = (i - 1) / 2;
            assert heap.get(i).priority() >= heap.get(p).priority();
            assert index.get(heap.get(i).key()) == i;
        }
        assert index.size() == heap.size();
        return true;
    }

    /**
     * Create an empty queue.
     */
    public HeapMinQueue() {
        index = new HashMap<>();
        heap = new ArrayList<>();
        assert checkInvariant();
    }

    /**
     * Return whether this queue contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Return the number of elements contained in this queue.
     */
    @Override
    public int size() {
        return heap.size();
    }

    /**
     * Return an element associated with the smallest priority in this queue.  This is the same
     * element that would be removed by a call to `remove()` (assuming no mutations in between).
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType get() {
        // Propagate exception from `List::getFirst()` if empty.
        return heap.getFirst().key();
    }

    /**
     * Return the minimum priority associated with an element in this queue.  Throws
     * NoSuchElementException if this queue is empty.
     */
    @Override
    public int minPriority() {
        return heap.getFirst().priority();
    }

    /**
     * If `key` is already contained in this queue, change its associated priority to `priority`.
     * Otherwise, add it to this queue with that priority.
     */
    @Override
    public void addOrUpdate(KeyType key, int priority) {
        if (!index.containsKey(key)) {
            add(key, priority);
        } else {
            update(key, priority);
        }
    }

    /**
     * Remove and return the element associated with the smallest priority in this queue.  If
     * multiple elements are tied for the smallest priority, an arbitrary one will be removed.
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        Entry<KeyType> first = heap.getFirst();
        Entry<KeyType> last = heap.removeLast();
        if (!isEmpty()) {
            heap.set(0, last);
            index.put(last.key(), 0);
            bubbleDown(0);
        }
        index.remove(first.key());
        return first.key();
    }

    /**
     * Remove all elements from this queue (making it empty).
     */
    @Override
    public void clear() {
        index.clear();
        heap.clear();
        assert checkInvariant();
    }

    /**
     * Swap the Entries at indices `i` and `j` in `heap`, updating `index` accordingly.  Requires `0
     * <= i,j < heap.size()`.
     */
    private void swap(int i, int j) {
        assert i >= 0 && i < heap.size();
        assert j >= 0 && j < heap.size();

        Entry<KeyType> tmp = heap.get(i);
        Entry<KeyType> tmp2 = heap.get(j);
        heap.set(i, tmp2);
        heap.set(j, tmp);
        index.put(heap.get(i).key(), i);
        index.put(heap.get(j).key(), j);
    }

    /**
     * Add element `key` to this queue, associated with priority `priority`.  Requires `key` is not
     * contained in this queue.
     */
    private void add(KeyType key, int priority) {
        assert !index.containsKey(key);

        Entry<KeyType> entry = new Entry<>(key, priority);
        heap.add(entry);
        index.put(key, heap.size() - 1);
        bubbleUp(heap.size() - 1);

        assert checkInvariant();
    }

    /**
     * Change the priority associated with element `key` to `priority`.  Requires that `key` is
     * contained in this queue.
     */
    private void update(KeyType key, int priority) {
        assert index.containsKey(key);
        int currIndex = index.get(key);
        Entry<KeyType> currEntry = heap.get(currIndex);

        heap.set(currIndex, new Entry<>(key, priority));

        if (priority < currEntry.priority()) {
            bubbleUp(currIndex);
        } else if (priority != currEntry.priority()) {
            bubbleDown(currIndex);
        }
        assert checkInvariant();
    }

    /**
     * Maintains the min heap invariant by moving the entry at the specified index up the heap until the
     * heap property is restored. This is typically called after inserting a new entry at the end of
     * the heap.
     */
    private void bubbleUp(int child) {
        int parent = (child - 1) / 2;
        while (child > 0 && heap.get(child).priority() < heap.get(parent).priority()) {
            swap(child, parent);
            child = parent;
            parent = (child - 1) / 2;
        }
    }

    /**
     * Maintains the heap invariant by moving the entry at the specified index down the heap until the
     * heap property is restored. This is typically called after removing the top entry or replacing
     * the top entry with the last entry in the heap.
     */
    private void bubbleDown(int parent) {
        int c = 2 * parent + 1;
        while (c < size()) {
            if (c + 1 < size() && heap.get(c + 1).priority() < heap.get(c).priority()) {
                c += 1;
            }

            if (heap.get(parent).priority() <= heap.get(c).priority()) {
                return;
            }
            swap(parent, c);
            parent = c;
            c = 2 * parent + 1;
        }
    }
}
