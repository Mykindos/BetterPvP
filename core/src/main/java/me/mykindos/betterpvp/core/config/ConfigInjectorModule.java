package me.mykindos.betterpvp.core.config;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.config.implementations.ConfigImpl;
import me.mykindos.betterpvp.core.config.implementations.ConfigProvider;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;


public class ConfigInjectorModule extends AbstractModule {

    private final BPvPPlugin plugin;
    private final Set<Field> fields;
    private final HashMap<String, ConfigProvider<?>> providers;

    public ConfigInjectorModule(BPvPPlugin plugin, Set<Field> fields) {
        this.plugin = plugin;
        this.fields = fields;
        this.providers = new HashMap<>();
    }

    @Override
    protected void configure() {

        for (var field : fields) {
            Config config = field.getAnnotation(Config.class);
            if (config == null) continue;

            Config conf = new ConfigImpl(config.path(), config.defaultValue());

            Class<?> type = field.getType();
            bind(type).annotatedWith(conf).toProvider(getProvider(config.path(), config.defaultValue(), type));

        }

    }

    @SuppressWarnings("unchecked")
    private <T> ConfigProvider<T> getProvider(String path, String defaultValue, Class<?> type) {
        ConfigProvider<?> provider;
        if (providers.containsKey(path)) {
            provider = providers.get(path);
        } else {
            provider = new ConfigProvider<>(plugin, path, defaultValue, type);
            providers.put(path, provider);
        }

        return (ConfigProvider<T>) provider;
    }

}
