package me.mykindos.betterpvp.core.framework.customtypes;

import lombok.Data;

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

}
