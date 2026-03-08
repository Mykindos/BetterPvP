package me.mykindos.betterpvp.core.item.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ConfigEntry<T> implements Supplier<T> {

    private final @NotNull Config config;
    @Getter
    private final @NotNull String key;
    private final @NotNull Class<T> type;
    private final @NotNull T defaultValue;
    private T value;

    public ConfigEntry(@NotNull Config config, @NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        this.config = config;
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        // initialize
        this.value = get();
    }

    /**
     * Get the current value from the config.
     */
    public void fetch() {
        this.value = get();
    }

    /**
     * Get the current value from the config.
     *
     * @return the config value, or the default if not set
     */
    public T get() {
        return config.getConfig(key, defaultValue, type);
    }

}
