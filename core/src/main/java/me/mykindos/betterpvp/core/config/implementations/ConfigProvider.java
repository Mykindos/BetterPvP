package me.mykindos.betterpvp.core.config.implementations;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

public class ConfigProvider<T> implements Provider<T> {

    private final BPvPPlugin plugin;
    private final String configPath;
    private final String defaultValue;
    private Class<T> type;

    public ConfigProvider(BPvPPlugin plugin, String configPath, String defaultValue, Class<T> type) {
        this.plugin = plugin;
        this.configPath = configPath;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {

        Object castedDefault = defaultValue;
        if(type == int.class) {
            castedDefault = Integer.parseInt(defaultValue);
            type = (Class<T>) Integer.class;
        }else if(type == double.class){
            castedDefault = Double.parseDouble(defaultValue);
            type = (Class<T>) Double.class;
        }else if(type == boolean.class){
            castedDefault = Boolean.parseBoolean(defaultValue);
            type = (Class<T>) Boolean.class;
        }else if(type == long.class) {
            castedDefault = Long.parseLong(defaultValue);
            type = (Class<T>) Long.class;
        }else if(type == List.class) {
            castedDefault = Arrays.asList(defaultValue.split(","));
        }

        T value = plugin.getConfig().getOrSaveObject(configPath, castedDefault, type);
        plugin.saveConfig();

        return value;
    }
}
