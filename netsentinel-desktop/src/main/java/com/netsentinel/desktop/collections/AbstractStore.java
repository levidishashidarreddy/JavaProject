package com.netsentinel.desktop.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract Generic Base Class forming a Generic Class Hierarchy:
 * FilterableStore<T> (Interface) -> AbstractStore<T> (Abstract Class) -> DataStore<T> (Concrete Class)
 */
public abstract class AbstractStore<T> implements FilterableStore<T> {

    // Demonstrates ArrayList implementation
    protected final List<T> internalList = new ArrayList<>();

    @Override
    public void add(T item) {
        if (item != null) {
            internalList.add(item);
        }
    }

    @Override
    public void addAll(Collection<? extends T> items) {
        if (items != null) {
            for (T item : items) {
                add(item);
            }
        }
    }

    @Override
    public int size() {
        return internalList.size();
    }

    @Override
    public void clear() {
        internalList.clear();
    }
}

