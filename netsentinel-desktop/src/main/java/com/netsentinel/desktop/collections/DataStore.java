package com.netsentinel.desktop.collections;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * =========================================================================
 * NETSENTINEL COLLECTIONS & GENERICS DEMONSTRATION STORE
 * =========================================================================
 * 
 * Demonstrates Core Java Syllabus Collections & Generics Concepts:
 * 1. ArrayList (linear dynamic buffer)
 * 2. LinkedList (doubly-linked queue/dequeue eviction buffer)
 * 3. HashSet (fast O(1) deduplication of observed entities)
 * 4. HashMap (key-value indexing of entities by identifier)
 * 5. TreeSet (sorted order collection with natural Comparable/Comparator)
 * 6. Iterator (explicit iterator navigation and safe item removal)
 * 7. Generic class & method definitions (<T>, <R>, <K, V>)
 * 8. Wildcard variance: <? extends T> (covariance) and <? super T> (contravariance)
 */
public class DataStore<T> extends AbstractStore<T> {

    // Demonstrates LinkedList for sliding timeline buffer
    private final LinkedList<T> timelineQueue = new LinkedList<>();

    // Demonstrates HashSet for unique identifier tracking
    private final Set<String> uniqueKeys = new HashSet<>();

    // Demonstrates HashMap for key-value lookups
    private final Map<String, T> indexMap = new HashMap<>();

    // Function to extract indexing key from generic item T
    private final Function<? super T, String> keyExtractor;

    public DataStore(Function<? super T, String> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void add(T item) {
        super.add(item);
        if (item != null) {
            // Add to LinkedList
            timelineQueue.addLast(item);

            // Index in HashMap and HashSet
            if (keyExtractor != null) {
                String key = keyExtractor.apply(item);
                if (key != null) {
                    uniqueKeys.add(key);
                    indexMap.put(key, item);
                }
            }
        }
    }

    /**
     * Demonstrates Iterator usage for safe conditional removal during traversal.
     */
    public int removeMatching(Predicate<? super T> filter) {
        int removedCount = 0;
        // Explicit iterator on internal ArrayList
        Iterator<T> iterator = internalList.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (filter.test(item)) {
                iterator.remove(); // Safe removal without ConcurrentModificationException
                removedCount++;
                if (keyExtractor != null) {
                    String key = keyExtractor.apply(item);
                    if (key != null) {
                        uniqueKeys.remove(key);
                        indexMap.remove(key);
                    }
                }
            }
        }

        // Re-synchronize LinkedList using Iterator
        Iterator<T> queueIt = timelineQueue.iterator();
        while (queueIt.hasNext()) {
            if (filter.test(queueIt.next())) {
                queueIt.remove();
            }
        }

        return removedCount;
    }

    /**
     * Demonstrates TreeSet for sorted unique extraction.
     */
    public <C extends Comparable<? super C>> TreeSet<C> extractSortedSet(Function<? super T, C> fieldExtractor) {
        TreeSet<C> sortedSet = new TreeSet<>();
        for (T item : internalList) {
            if (item != null) {
                C val = fieldExtractor.apply(item);
                if (val != null) {
                    sortedSet.add(val);
                }
            }
        }
        return sortedSet;
    }

    @Override
    public List<T> filter(Predicate<? super T> predicate) {
        List<T> result = new ArrayList<>();
        // Traditional Iterator loop for syllabus demonstration
        Iterator<T> it = internalList.iterator();
        while (it.hasNext()) {
            T item = it.next();
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public <R> List<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = new ArrayList<>(internalList.size());
        for (T item : internalList) {
            mapped.add(mapper.apply(item));
        }
        return mapped;
    }

    public T findByKey(String key) {
        return indexMap.get(key);
    }

    public boolean containsKey(String key) {
        return uniqueKeys.contains(key);
    }

    public List<T> getRecentFromLinkedList(int count) {
        List<T> recent = new ArrayList<>();
        int take = Math.min(count, timelineQueue.size());
        // Walk backwards from LinkedList tail
        for (int i = 0; i < take; i++) {
            recent.add(timelineQueue.get(timelineQueue.size() - 1 - i));
        }
        return recent;
    }

    public List<T> getAll() {
        return new ArrayList<>(internalList);
    }

    public Set<String> getUniqueKeys() {
        return Collections.unmodifiableSet(uniqueKeys);
    }
}

