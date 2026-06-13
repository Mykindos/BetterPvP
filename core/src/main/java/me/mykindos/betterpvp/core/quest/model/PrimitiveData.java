package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * A configured primitive instance (trigger / condition / action / reward) as
 * authored in the console: a {@code type} id plus a free-form parameter map.
 * The handler registry interprets {@code type}; these accessors read params.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimitiveData {

    private String id;
    private String type;
    private Map<String, Object> params = new HashMap<>();

    public String getString(String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public int getInt(String key, int fallback) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
