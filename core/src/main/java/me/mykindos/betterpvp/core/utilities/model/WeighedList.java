package me.mykindos.betterpvp.core.utilities.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Represents a list of categories with weights, and elements within those categories with weights.
 * When obtaining a random element, the category is chosen by its weight first, then the element
 * within that category is chosen by its weight as well.
 * <p>
 * The effective probability of getting an element from the whole pool is:
 * <p>
 * <code>
 * (Weight of the category / Total weights of all categories) *
 * (Weight of the element / Total weights of all elements in that category)
 * </code>.
 * @param <T> The type of element to store
 */
public class WeighedList<T> {

    private final Random rnd = new Random();
    private final Map<Integer, Multimap<Integer, T>> map = new HashMap<>();

    public void add(int categoryWeight, int weight, T element) {
        map.computeIfAbsent(categoryWeight, k -> ArrayListMultimap.create()).put(weight, element);
    }

    public Map<Integer, Multimap<Integer, T>> getMap() {
        return map;
    }

    public Collection<T> getElements() {
        return map.values().stream().flatMap(m -> m.values().stream()).toList();
    }

    /**
     * Gets a random element from the whole pool.
     * @return A random element from the whole pool.
     */
    public T random() {
        if (map.isEmpty()) {
            return null;
        }

        int totalWeight = map.keySet().stream().mapToInt(Integer::intValue).sum();
        int random = rnd.nextInt(totalWeight);
        for (int weight : map.keySet()) {
            if (random < weight) {
                return random(weight);
            }
            random -= weight;
        }

        throw new IllegalStateException();
    }

    /**
     * Gets a random element from the category with the specified weight.
     * @param weight The weight of the elements to choose from.
     * @return A random element from the category with the specified weight.
     */
    public T random(int weight) {
        Multimap<Integer, T> multimap = map.get(weight);
        int totalWeight = multimap.keySet().stream().mapToInt(Integer::intValue).sum();
        int random = rnd.nextInt(totalWeight);
        for (int key : multimap.keySet()) {
            if (random <= key) {
                final Object[] pool = multimap.get(key).toArray();
                return (T) pool[rnd.nextInt(pool.length)];
            }
            random -= key;
        }
        return null;
    }

    public int getTotalCategoryWeights() {
        return map.keySet().stream().mapToInt(Integer::intValue).sum();
    }

}
