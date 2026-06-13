package me.mykindos.betterpvp.core.quest.cinematic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/** A keyframe on a cinematic track at a given tick. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinKeyframe {

    private String id;
    private int tick;
    private Map<String, Object> data = new HashMap<>();

    public double getDouble(String key, double fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
