package me.mykindos.betterpvp.core.config;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import me.mykindos.betterpvp.core.config.implementations.ConfigImpl;
import me.mykindos.betterpvp.core.config.implementations.ConfigProvider;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ConfigInjectorModule extends AbstractModule {

    private final BPvPPlugin plugin;
    private final Set<Field> fields;
    private final HashMap<String, ConfigProvider<?>> providers;
    private final Adapters adapters;

    public ConfigInjectorModule(BPvPPlugin plugin, Set<Field> fields) {
        this.plugin = plugin;
        this.fields = fields;
        this.providers = new HashMap<>();
        this.adapters = new Adapters(plugin);
    }

    @Override
    protected void configure() {

        for (var field : fields) {
            if (!this.adapters.canLoad(field.getDeclaringClass())) {
                continue; // Skip if the class is not supported
            }
            Config config = field.getAnnotation(Config.class);
            if (config == null) continue;

            Config conf = new ConfigImpl(config.path(), config.defaultValue(), config.configName());

            Class<?> type = field.getType();
            if (type == List.class) {
                bind(new TypeLiteral<List<String>>() {}).annotatedWith(conf).toProvider(getProvider(config.path(), config.defaultValue(), type, config.configName()));
            } else {
                bind(type).annotatedWith(conf).toProvider(getProvider(config.path(), config.defaultValue(), type, config.configName()));
            }
        }

    }

    @SuppressWarnings("unchecked")
    private <T> ConfigProvider<T> getProvider(String path, String defaultValue, Class<?> type, String configName) {
        ConfigProvider<?> provider;
        if (providers.containsKey(path)) {
            provider = providers.get(path);
        } else {
            provider = new ConfigProvider<>(plugin, path, defaultValue, type, configName);
            providers.put(path, provider);
        }

        return (ConfigProvider<T>) provider;
    }

    public void reload() {

    }

}
