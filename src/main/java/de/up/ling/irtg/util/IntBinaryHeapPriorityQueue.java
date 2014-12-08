package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.*;

/**
 * PriorityQueue with explicit double priority values. Larger doubles are higher
 * priorities. BinaryHeap-backed.
 *
 * For each entry, uses ~ 24 (entry) + 16? (Map.Entry) + 4 (List entry) = 44
 * bytes?
 *
 * @author Dan Klein
 * @author Christopher Manning
 * @author Modified to an int-only PriorityQueue by Johannes Gontrum
 */
public class IntBinaryHeapPriorityQueue implements IntPriorityQueue, IntIterator {

    @Override
    public <T> T[] toArray(T[] ts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int[] toIntArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int[] toIntArray(int[] ints) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int[] toArray(int[] ints) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean rem(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsAll(IntCollection ic) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(IntCollection ic) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean retainAll(IntCollection ic) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int skip(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasNext() {
        return size() > 0;
    }

    @Override
    public int nextInt() {
        if (size() == 0) {
            throw new NoSuchElementException("Empty PQ");
        }
        return removeFirst();
    }

    @Override
    public Integer next() {
        return nextInt();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * An {@code Entry} stores an object in the queue along with its current
     * location (array position) and priority. uses ~ 8 (self) + 4 (key ptr) + 4
     * (index) + 8 (priority) = 24 bytes?
     */
    private static final class IntEntry {

        public int key;
        public int index;
        public double priority;

        @Override
        public String toString() {
            return key + " at " + index + " (" + priority + ')';
        }
    }

    /**
     * {@code indexToEntry} maps linear array locations (not priorities) to heap
     * entries.
     */
    private final List<IntEntry> indexToEntry;

    /**
     * {@code keyToEntry} maps heap objects to their heap entries.
     */
    private final Int2ObjectMap<IntEntry> keyToEntry;

    private IntEntry parent(IntEntry entry) {
        int index = entry.index;
        System.err.println("ge " + (index - 1) / 2);
        return (index > 0 ? getEntry((index - 1) / 2) : null);
    }

    private IntEntry leftChild(IntEntry entry) {
        int leftIndex = entry.index * 2 + 1;
        return (leftIndex < size() ? getEntry(leftIndex) : null);
    }

    private IntEntry rightChild(IntEntry entry) {
        int index = entry.index;
        int rightIndex = index * 2 + 2;
        return (rightIndex < size() ? getEntry(rightIndex) : null);
    }

    private int compare(IntEntry entryA, IntEntry entryB) {
//        if (entryA == null) {
//            return -1;
//        } else if (entryB == null) {
//            return 1;
//        }
        int result = compare(entryA.priority, entryB.priority);
        if (result != 0) {
            return result;
        }

        return (entryA.key > entryB.key ? 1 : (entryA.key < entryB.key ? -1 : 0));
    }

    private static int compare(double a, double b) {
        double diff = a - b;
        if (diff > 0.0) {
            return 1;
        }
        if (diff < 0.0) {
            return -1;
        }
        return 0;
    }

    /**
     * Structural swap of two entries.
     *
     */
    private void swap(IntEntry entryA, IntEntry entryB) {
        int indexA = entryA.index;
        int indexB = entryB.index;
        entryA.index = indexB;
        entryB.index = indexA;
        indexToEntry.set(indexA, entryB);
        indexToEntry.set(indexB, entryA);
    }

    /**
     * Remove the last element of the heap (last in the index array).
     */
    private void removeLastEntry() {
        IntEntry entry = indexToEntry.remove(size() - 1);
        keyToEntry.remove(entry.key);
    }

    /**
     * Get the entry by key (null if none).
     */
    private IntEntry getEntry(int key) {
        return keyToEntry.get(key);
    }

    /**
     * Get entry by index, exception if none.
     */
    private IntEntry getEntryByIndex(int index) {
        IntEntry entry = indexToEntry.get(index);
        return entry;
    }

    private IntEntry makeEntry(int key) {
        IntEntry entry = new IntEntry();
        entry.index = size();
        entry.key = key;
        entry.priority = Double.NEGATIVE_INFINITY;
        indexToEntry.add(entry);
        keyToEntry.put(key, entry);
        return entry;
    }

    /**
     * iterative heapify up: move item o at index up until correctly placed
     */
    private void heapifyUp(IntEntry entry) {
        while (true) {
            System.err.println(entry.index);
            if (entry.index == 0) {
                break;
            }
            IntEntry parentEntry = parent(entry);
//            if (parentEntry == null) {
//                break; //!
//            }
            if (compare(entry, parentEntry) <= 0) {
                break;
            }
            swap(entry, parentEntry);
        }
    }

    /**
     * On the assumption that leftChild(entry) and rightChild(entry) satisfy the
     * heap property, make sure that the heap at entry satisfies this property
     * by possibly percolating the element entry downwards. I've replaced the
     * obvious recursive formulation with an iterative one to gain (marginal)
     * speed
     */
    private void heapifyDown(final IntEntry entry) {
        IntEntry bestEntry; // initialized below

        do {
            bestEntry = entry;

            IntEntry leftEntry = leftChild(entry);
            if (leftEntry != null) {
                if (compare(bestEntry, leftEntry) < 0) {
                    bestEntry = leftEntry;
                }
            }

            IntEntry rightEntry = rightChild(entry);
            if (rightEntry != null) {
                if (compare(bestEntry, rightEntry) < 0) {
                    bestEntry = rightEntry;
                }
            }

            if (bestEntry != entry) {
                // Swap min and current
                swap(bestEntry, entry);
        // at start of next loop, we set currentIndex to largestIndex
                // this indexation now holds current, so it is unchanged
            }
        } while (bestEntry != entry);
    // System.err.println("Done with heapify down");
        // verify();
    }

    private void heapify(IntEntry entry) {
        heapifyUp(entry);
        heapifyDown(entry);
    }

    /**
     * Finds the E with the highest priority, removes it, and returns it.
     *
     * @return the E with highest priority
     */
    @Override
    public int removeFirst() {
        int first = getFirst();
        remove(first);
        return first;
    }

    /**
     * Finds the E with the highest priority and returns it, without modifying
     * the queue.
     *
     * @return the E with minimum key
     */
    @Override
    public int getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return getEntry(0).key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPriority() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return getEntry(0).priority;
    }

    /**
     * Searches for the object in the queue and returns it. May be useful if you
     * can create a new object that is .equals() to an object in the queue but
     * is not actually identical, or if you want to modify an object that is in
     * the queue.
     *
     * @return null if the object is not in the queue, otherwise returns the
     * object.
     */
    public int getObject(int key) {
        if (!contains(key)) {
            return -1;
        }
        IntEntry e = getEntry(key);
        return e.key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPriority(int key) {
        IntEntry entry = getEntry(key);
        if (entry == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return entry.priority;
    }

    @Override
    public boolean add(Integer e) {
        return add((int) e);
    }

    /**
     * Adds an object to the queue with the minimum priority
     * (Double.NEGATIVE_INFINITY). If the object is already in the queue with
     * worse priority, this does nothing. If the object is already present, with
     * better priority, it will NOT cause an a decreasePriority.
     *
     * @param key an <code>E</code> value
     * @return whether the key was present before
     */
    @Override
    public boolean add(int key) {
        if (contains(key)) {
            return false;
        }
        makeEntry(key);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(int key, double priority) {
//    System.err.println("Adding " + key + " with priority " + priority);
        if (add(key)) {
            relaxPriority(key, priority);
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(IntCollection ic) {
        IntIterator it = ic.iterator();
        boolean status;

        while (it.hasNext()) {
            status = add(it.nextInt());
            if (status == false) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        boolean status = false;

        for (Integer item : c) {
            status = add(item);
            if (status == false) {
                return false;
            }
        }

        return status;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(int key) {
        IntEntry entry = getEntry(key);
        if (entry == null) {
            return false;
        }
        removeEntry(entry);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return remove((int) o);
    }

    private void removeEntry(IntEntry entry) {
        IntEntry lastEntry = getLastEntry();
        if (entry != lastEntry) {
            swap(entry, lastEntry);
            removeLastEntry();
            heapify(lastEntry);
        } else {
            removeLastEntry();
        }
    }

    private IntEntry getLastEntry() {
        return getEntry(size() - 1);
    }

    /**
     * Promotes a key in the queue, adding it if it wasn't there already. If the
     * specified priority is worse than the current priority, nothing happens.
     * Faster than add if you don't care about whether the key is new.
     *
     * @param key an <code>Object</code> value
     * @return whether the priority actually improved.
     */
    @Override
    public boolean relaxPriority(int key, double priority) {
        System.err.println("keyToEntry: " + keyToEntry.toString());
        IntEntry entry = getEntry(key);
        if (entry == null) {
            System.err.println("Making new");
            entry = makeEntry(key);
        }
        System.err.println("p: " + priority);
        System.err.println("ep: " + entry.priority);

        if (compare(priority, entry.priority) <= 0) {
            return false;
        }
        entry.priority = priority;
        heapifyUp(entry);
        return true;
    }

    /**
     * Demotes a key in the queue, adding it if it wasn't there already. If the
     * specified priority is better than the current priority, nothing happens.
     * If you decrease the priority on a non-present key, it will get added, but
     * at it's old implicit priority of Double.NEGATIVE_INFINITY.
     *
     * @param key an <code>Object</code> value
     * @return whether the priority actually improved.
     */
    public boolean decreasePriority(int key, double priority) {
        IntEntry entry = getEntry(key);
        if (entry == null) {
            entry = makeEntry(key);
        }
        if (compare(priority, entry.priority) >= 0) {
            return false;
        }
        entry.priority = priority;
        heapifyDown(entry);
        return true;
    }

    /**
     * Changes a priority, either up or down, adding the key it if it wasn't
     * there already.
     *
     * @param key an <code>Object</code> value
     * @return whether the priority actually changed.
     */
    @Override
    public boolean changePriority(int key, double priority) {
        IntEntry entry = getEntry(key);
        if (entry == null) {
            entry = makeEntry(key);
        }
        if (compare(priority, entry.priority) == 0) {
            return false;
        }
        entry.priority = priority;
        heapify(entry);
        return true;
    }

    /**
     * Checks if the queue is empty.
     *
     * @return a <code>boolean</code> value
     */
    @Override
    public boolean isEmpty() {
        return indexToEntry.isEmpty();
    }

    /**
     * Get the number of elements in the queue.
     *
     * @return queue size
     */
    @Override
    public int size() {
        return indexToEntry.size();
    }

    /**
     * Returns whether the queue contains the given key.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean contains(int key) {
        return keyToEntry.containsKey(key);
    }

    @Override
    public boolean contains(Object o) {
        return contains((int) o);
    }

    @Override
    public IntList toSortedList() {
        IntList sortedList = new IntArrayList(size());
        IntBinaryHeapPriorityQueue queue = this.deepCopy();
        while (!queue.isEmpty()) {
            sortedList.add(queue.removeFirst());
        }
        return sortedList;
    }

    public IntBinaryHeapPriorityQueue deepCopy(Int2ObjectMapFactory<IntEntry> mapFactory) {
        IntBinaryHeapPriorityQueue queue
                = new IntBinaryHeapPriorityQueue(mapFactory);
        keyToEntry.values().stream().forEach((entry) -> {
            queue.relaxPriority(entry.key, entry.priority);
        });
        return queue;
    }

    public IntBinaryHeapPriorityQueue deepCopy() {
        return deepCopy(Int2ObjectMapFactory.<IntEntry>Int2ObjectHashMapFactory());
    }

    @Override
    public IntIterator intIterator() {
        return toSortedList().iterator();
    }

    @Override
    public IntIterator iterator() {
        return intIterator();
    }

    /**
     * Clears the queue.
     */
    @Override
    public void clear() {
        indexToEntry.clear();
        keyToEntry.clear();
    }

  //  private void verify() {
    //    for (int i = 0; i < indexToEntry.size(); i++) {
    //      if (i != 0) {
    //        // check ordering
    //        if (compare(getEntry(i), parent(getEntry(i))) < 0) {
    //          System.err.println("Error in the ordering of the heap! ("+i+")");
    //          System.exit(0);
    //        }
    //      }
    //      // check placement
    //      if (i != ((Entry)indexToEntry.get(i)).index)
    //        System.err.println("Error in placement in the heap!");
    //    }
    //  }
    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(int maxKeysToPrint) {
        if (maxKeysToPrint <= 0) {
            maxKeysToPrint = Integer.MAX_VALUE;
        }
        IntList sortedKeys = toSortedList();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < maxKeysToPrint && i < sortedKeys.size(); i++) {
            int key = sortedKeys.getInt(i);
            sb.append(key).append('=').append(getPriority(key));
            if (i < maxKeysToPrint - 1 && i < sortedKeys.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public String toVerticalString() {
        IntList sortedKeys = toSortedList();
        StringBuilder sb = new StringBuilder();
        for (IntIterator keyI = sortedKeys.iterator(); keyI.hasNext();) {
            int key = keyI.nextInt();
            sb.append(key);
            sb.append('\t');
            sb.append(getPriority(key));
            if (keyI.hasNext()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public IntBinaryHeapPriorityQueue() {
        this(Int2ObjectMapFactory.<IntEntry>Int2ObjectHashMapFactory());
    }

    public IntBinaryHeapPriorityQueue(int initCapacity) {
        this(Int2ObjectMapFactory.<IntEntry>Int2ObjectHashMapFactory(), initCapacity);
    }

    public IntBinaryHeapPriorityQueue(Int2ObjectMapFactory<IntEntry> mapFactory) {
        indexToEntry = new ArrayList<IntEntry>();
        keyToEntry = mapFactory.newMap();
    }

    public IntBinaryHeapPriorityQueue(Int2ObjectMapFactory<IntEntry> mapFactory, int initCapacity) {
        indexToEntry = new ArrayList<IntEntry>(initCapacity);
        keyToEntry = mapFactory.newMap(initCapacity);
    }

    
    public static void main(String[] args) {
            IntBinaryHeapPriorityQueue testqueue = new IntBinaryHeapPriorityQueue();
            System.err.println(testqueue.relaxPriority(10, 0.1));
            System.err.println(testqueue.relaxPriority(9, 0.2));
            testqueue.relaxPriority(8, 0.3);
            testqueue.relaxPriority(7, 0.4);
            testqueue.relaxPriority(6, 0.5);
            testqueue.relaxPriority(5, 0.6);
            testqueue.relaxPriority(4, 0.7);
            testqueue.relaxPriority(3, 0.8);
            testqueue.relaxPriority(2, 0.9);
            testqueue.relaxPriority(1, 1.0);
            
            while (testqueue.hasNext()) {
                int i = testqueue.removeFirst();
                System.err.println(i);
            }
    }
}
