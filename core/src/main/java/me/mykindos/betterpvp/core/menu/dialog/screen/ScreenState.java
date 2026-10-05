package me.mykindos.betterpvp.core.menu.dialog.screen;

import java.util.HashMap;
import java.util.Map;

/**
 * The values one open screen reads. Bindings look keys up here, built-in {@code set} actions and handlers change
 * them, and fields write their values into the keys they bind.
 */
public class ScreenState {

    private final Map<String, Object> values;

    public ScreenState(Map<String, Object> values) {
        this.values = new HashMap<>(values);
    }

    public Object get(String key) {
        return values.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T fallback) {
        final Object value = values.get(key);
        return value == null ? fallback : (T) value;
    }

    public ScreenState set(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
