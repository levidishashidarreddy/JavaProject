package com.netsentinel.desktop.collections;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic interface demonstrating Java Collections & Generics Syllabus concepts.
 * 
 * Features:
 * - Generic Interface with type parameter T
 * - Bounded Wildcards (<? extends T> and <? super T>)
 * - Generic Methods with independent type parameters <R>
 */
public interface FilterableStore<T> {

    void add(T item);

    // Bounded wildcard <? extends T> allows adding collections of any subtype of T
    void addAll(Collection<? extends T> items);

    // Generic method with predicate parameter
    List<T> filter(Predicate<? super T> predicate);

    // Generic method with transformation mapping <? super T> to <? extends R>
    <R> List<R> map(Function<? super T, ? extends R> mapper);

    int size();

    void clear();
}

