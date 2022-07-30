package me.mykindos.betterpvp.core.config.implementations;

import com.google.inject.Provider;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;

public class ConfigProvider<T> implements Provider<T> {

    private final BPvPPlugin plugin;
    private final String configPath;
    private final Class<T> type;

    public ConfigProvider(BPvPPlugin plugin, String configPath, Class<T> type) {
        this.plugin = plugin;
        this.configPath = configPath;
        this.type = type;
    }

    @Override
    public T get() {
        return plugin.getConfig().getObject(configPath, type);
    }
}
