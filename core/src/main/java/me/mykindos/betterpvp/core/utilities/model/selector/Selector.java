package me.mykindos.betterpvp.core.utilities.model.selector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A generic selector interface that selects elements of type T.
 *
 * @param <T> the type of elements to select
 */
@FunctionalInterface
public interface Selector<T> {

    /**
     * Selects and returns a collection of elements.
     *
     * @return a collection of selected elements
     */
    Collection<T> select();

    /**
     * Creates a new selector that combines the results of this selector and another selector.
     * The resulting selector will return all unique elements from both selectors.
     *
     * @param other the other selector to combine with
     * @return a new selector returning the union of both selectors' results
     */
    default Selector<T> union(Selector<T> other) {
        return () -> {
            Set<T> result = new HashSet<>(this.select());
            result.addAll(other.select());
            return result;
        };
    }

    /**
     * Creates a new selector that returns only elements present in both this selector and another selector.
     *
     * @param other the other selector to intersect with
     * @return a new selector returning only common elements
     */
    default Selector<T> intersect(Selector<T> other) {
        return () -> {
            Set<T> result = new HashSet<>(this.select());
            result.retainAll(other.select());
            return result;
        };
    }
}
