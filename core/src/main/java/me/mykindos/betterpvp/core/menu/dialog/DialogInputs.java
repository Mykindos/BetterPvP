package me.mykindos.betterpvp.core.menu.dialog;

import java.util.Map;

/**
 * Input values a click carried, by field key. Clicks on canvas regions carry none, native buttons carry every field.
 */
public class DialogInputs {

    public static final DialogInputs EMPTY = new DialogInputs(Map.of());

    private final Map<String, Object> values;

    public DialogInputs(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public String text(String key) {
        return (String) values.get(key);
    }

    public Boolean toggle(String key) {
        return (Boolean) values.get(key);
    }

    public Float number(String key) {
        return (Float) values.get(key);
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
