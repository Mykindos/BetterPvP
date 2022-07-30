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

    public ConfigInjectorModule(BPvPPlugin plugin, Set<Field> fields){
        this.plugin = plugin;
        providers = new HashMap<>();
        this.fields = fields;
    }

    @Override
    protected void configure() {

        for (var field : fields) {
            Config config = field.getAnnotation(Config.class);
            if(config == null) continue;

            // Make sure we save it as castable types
            var set = plugin.getConfig().isSet(config.path());

            Config conf = new ConfigImpl(config.path(), config.defaultValue());

            if (field.getType().isAssignableFrom(String.class)) {
                if(!set){
                    plugin.getConfig().set(config.path(), config.defaultValue());
                }
                bind(String.class).annotatedWith(conf).toProvider(getProvider(config.path(), String.class));
            } else if (field.getType().isAssignableFrom(int.class)) {
                if(!set){
                    plugin.getConfig().set(config.path(), Integer.parseInt(config.defaultValue()));
                }
                bind(int.class).annotatedWith(conf).toProvider(getProvider(config.path(), Integer.class));
            } else if (field.getType().isAssignableFrom(boolean.class)) {
                if(!set){
                    plugin.getConfig().set(config.path(), Boolean.parseBoolean(config.defaultValue()));
                }
                bind(boolean.class).annotatedWith(conf).toProvider(getProvider(config.path(), Boolean.class));
            }

        }

        plugin.saveConfig();
    }

    @SuppressWarnings("unchecked")
    private <T> ConfigProvider<T> getProvider(String path, Class<?> type){
        ConfigProvider<?> provider;
        if(providers.containsKey(path)){
            provider =  providers.get(path);
        }else{
            provider = new ConfigProvider<>(plugin, path, type);
            providers.put(path, provider);
        }

        return (ConfigProvider<T>) provider;
    }

}
