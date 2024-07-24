package me.mykindos.betterpvp.core.config.implementations;

import jakarta.inject.Provider;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.apache.commons.lang.ClassUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@CustomLog
public class ConfigProvider<T> implements Provider<T> {

    private final BPvPPlugin plugin;
    private final String configPath;
    private final String defaultValue;
    private Class<T> type;
    private final String configName;

    private static final Map<Class<?>, Function<String, ?>> parsers = new HashMap<>();

    static {
        parsers.put(int.class, Integer::parseInt);
        parsers.put(double.class, Double::parseDouble);
        parsers.put(boolean.class, Boolean::parseBoolean);
        parsers.put(float.class, Float::parseFloat);
        parsers.put(long.class, Long::parseLong);
        parsers.put(List.class, s -> Arrays.asList(s.split(",")));
    }

    public ConfigProvider(BPvPPlugin plugin, String configPath, String defaultValue, Class<T> type, String configName) {
        this.plugin = plugin;
        this.configPath = configPath;
        this.type = type;
        this.defaultValue = defaultValue;
        this.configName = configName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        Object castedDefault = defaultValue;
        if (parsers.containsKey(type)) {
            try {
                castedDefault = parsers.get(type).apply(defaultValue);
                type = (Class<T>) ClassUtils.primitiveToWrapper(type);
            } catch (Exception ex) {
                log.error("Failed to parse default value for {} ({})", configPath, type.getSimpleName(), ex).submit();
            }
        }


        T value = plugin.getConfig(configName).getOrSaveObject(configPath, castedDefault, type);
        plugin.saveConfig();

        return value;
    }

}
