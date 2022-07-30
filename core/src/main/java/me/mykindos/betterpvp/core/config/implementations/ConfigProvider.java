package me.mykindos.betterpvp.core.config.implementations;

import com.google.inject.Provider;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;

public class ConfigProvider<T> implements Provider<T> {

    private final BPvPPlugin plugin;
    private final String configPath;
    private final String defaultValue;
    private final Class<T> type;

    public ConfigProvider(BPvPPlugin plugin, String configPath, String defaultValue, Class<T> type) {
        this.plugin = plugin;
        this.configPath = configPath;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public T get() {

        Object castedDefault = defaultValue;
        if(type == Integer.class){
            castedDefault = Integer.parseInt(defaultValue);
        }else if(type == Boolean.class){
            castedDefault = Boolean.parseBoolean(defaultValue);
        }

        return plugin.getConfig().getOrSaveObject(configPath, castedDefault, type);
    }
}
