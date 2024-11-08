package me.mykindos.betterpvp.core.framework.customtypes;

import lombok.Data;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

@Data
public class KeyValue<T, K> {

    private T key;
    private K value;

    public KeyValue(T key, K value) {
        this.key = key;
        this.value = value;
    }

    public T get() {
        return key;
    }

    /**
     * Returns a comparator that compares {@link KeyValue} in natural order on value.
     * Identical implementation as {@link Map.Entry#comparingByValue()}
     *
     * <p>The returned comparator is serializable and throws {@link
     * NullPointerException} when comparing an entry with null values.
     *
     * @param <K> the type of the key
     * @param <V> the {@link Comparable} type of the value
     * @return a comparator that compares {@link KeyValue} in natural order on value.
     * @see Comparable
     * @see Map.Entry#comparingByValue()
     */
    public static <K, V extends Comparable<? super V>> Comparator<KeyValue<K, V>> comparingByValue() {
        return (Comparator<KeyValue<K, V>> & Serializable)
                (c1, c2) -> c1.getValue().compareTo(c2.getValue());
    }

}
