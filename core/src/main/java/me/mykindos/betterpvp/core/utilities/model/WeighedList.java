package me.mykindos.betterpvp.core.utilities.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

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
public class WeighedList<T> implements Iterable<T>{

    private final Random rnd = new Random();
    private final Map<Integer, Multimap<Integer, T>> map;

    public WeighedList() {
        this.map = new HashMap<>();
    }

    public WeighedList(WeighedList<T> other) {
        this.map = new HashMap<>();
        for (Map.Entry<Integer, Multimap<Integer, T>> entry : other.map.entrySet()) {
            this.map.put(entry.getKey(), ArrayListMultimap.create(entry.getValue()));
        }
    }

    public void add(int categoryWeight, int weight, T element) {
        map.computeIfAbsent(categoryWeight, k -> ArrayListMultimap.create()).put(weight, element);
    }

    public Map<Integer, Multimap<Integer, T>> getMap() {
        return map;
    }

    public List<T> getElements() {
        return map.values().stream().flatMap(m -> m.values().stream()).toList();
    }

    public int size() {
        return map.values().stream().mapToInt(Multimap::size).sum();
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

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return getElements().iterator();
    }

    public void clear() {
        map.clear();
    }

    /**
     * <p>
     * Get the actual percentage to get a specific element from a list
     * </p>
     * <code>
     * (Weight of the category / Total weights of all categories) *
     * (Weight of the element / Total weights of all elements in that category)
     * </code>
     * @return
     */
    public Map<T, Float> getAbsoluteElementChances() {
        Map<T, Float> chancesMap = new WeakHashMap<>();
        this.map.forEach((categoryWeight, elementMultiMap) -> {
            float categoryMultiplier = (float) categoryWeight/getTotalCategoryWeights();
            float totalElementWeights = elementMultiMap.keys().stream().mapToInt(Integer::intValue).sum();
            elementMultiMap.forEach((weight, element) -> {
                chancesMap.put(element, categoryMultiplier * (weight/totalElementWeights));
            });

        });
        return chancesMap;
    }

    public String getAbsoluteElementChances(Function<T, String> elementAsString) {
        List<String> chancesInfo = new ArrayList<>();

        getAbsoluteElementChances().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> {
                T element = entry.getKey();
                float percentage = entry.getValue() * 100;
                chancesInfo.add(elementAsString.apply(element) + ": " + percentage + "%");
            });
        return String.join("\n", chancesInfo);
    }

    /**
     * Get the chance of getting a specified category, and the relative chances of the elements in that category
     * @return A Map, first element is the category weight, and chance to get, second element is a map of that categories
     * elements with their relative chance
     */
    public Map<KeyValue<Integer, Float>, Map<T, Float>> getFullChances() {
        Map<KeyValue<Integer, Float>, Map<T, Float>> chancesMap = new WeakHashMap<>();
        this.map.forEach((categoryWeight, elementMultiMap) -> {
            float categoryChance = (float) categoryWeight/getTotalCategoryWeights();
            float totalElementWeights = elementMultiMap.keys().stream().mapToInt(Integer::intValue).sum();

            KeyValue<Integer, Float> categoryWeightKV = new KeyValue<>(categoryWeight, categoryChance);
            Map<T, Float> categoryElementMap = new WeakHashMap<>();

            elementMultiMap.forEach((weight, element) -> {
                categoryElementMap.put(element, (weight/totalElementWeights));
            });

            chancesMap.put(categoryWeightKV, categoryElementMap);

        });
        return chancesMap;
    }

    public String getFullChances(Function<T, String> elementAsString) {
        List<String> chancesInfo = new ArrayList<>();

        getFullChances().entrySet().stream()
                //sort the map, by weight percentage
                .sorted(Map.Entry.comparingByKey(KeyValue.comparingByValue()))
                .forEach(keyValueMapEntry -> {
                    KeyValue<Integer, Float> keyValue = keyValueMapEntry.getKey();
                    chancesInfo.add("Category (" + keyValue.getKey() + "): " + keyValue.getValue() * 100 + "%");
                    //sort category on relative percentage
                    keyValueMapEntry.getValue().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .forEach(entry -> {
                                chancesInfo.add("\t" + elementAsString.apply(entry.getKey()) + ": " + entry.getValue() * 100 + "%");
                            });
                });
        return String.join("\n", chancesInfo);
    }
}
