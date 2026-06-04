package me.mykindos.betterpvp.clans.world.resource;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Typed view over a Mapper region's {@code getOptions().getTags()} ({@code Set<String>}).
 * <p>
 * Two tag shapes are understood: bare markers (e.g. {@code "tree"}, {@code "ore"}) and {@code key:value} pairs
 * (e.g. {@code "level:67"}, {@code "node:willow"}, {@code "name:Willow Tree"}). Keys are matched case-insensitively;
 * values keep their original case. This lets a resource node be selected and parameterised entirely from the region.
 */
public final class RegionTags {

    private final Set<String> markers = new HashSet<>();
    private final Map<String, String> values = new HashMap<>();

    public RegionTags(@NotNull Set<String> tags) {
        for (String raw : tags) {
            if (raw == null) {
                continue;
            }
            final String tag = raw.trim();
            if (tag.isEmpty()) {
                continue;
            }
            final int separator = tag.indexOf(':');
            if (separator > 0 && separator < tag.length() - 1) {
                values.put(tag.substring(0, separator).trim().toLowerCase(Locale.ROOT), tag.substring(separator + 1).trim());
            } else {
                markers.add(tag.toLowerCase(Locale.ROOT));
            }
        }
    }

    /**
     * @return true if {@code tag} is present either as a bare marker or as the key of a {@code key:value} pair
     */
    public boolean has(@NotNull String tag) {
        final String key = tag.toLowerCase(Locale.ROOT);
        return markers.contains(key) || values.containsKey(key);
    }

    public @NotNull Optional<String> get(@NotNull String key) {
        return Optional.ofNullable(values.get(key.toLowerCase(Locale.ROOT)));
    }

    public @NotNull String getString(@NotNull String key, @NotNull String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public int getInt(@NotNull String key, int defaultValue) {
        final String value = values.get(key.toLowerCase(Locale.ROOT));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    public double getDouble(@NotNull String key, double defaultValue) {
        final String value = values.get(key.toLowerCase(Locale.ROOT));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * @return the bare marker tags, lower-cased and unmodifiable
     */
    public @NotNull Set<String> markers() {
        return Collections.unmodifiableSet(markers);
    }
}
