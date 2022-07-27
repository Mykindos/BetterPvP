package me.mykindos.betterpvp.core.config;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.implementations.ConfigImpl;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import java.lang.reflect.Field;
import java.util.Set;


public class CoreConfigInjectorModule extends AbstractModule {

    private final Core core;
    private final String packageName;

    public CoreConfigInjectorModule(Core core, String packageName){
        this.core = core;
        this.packageName = packageName;
    }

    @Override
    protected void configure() {
        Reflections reflections = new Reflections(packageName, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);
        for (var field : fields) {
            Config config = field.getAnnotation(Config.class);
            if (config == null) continue;

            var existingValue = core.getConfig().get(config.path());
            if (existingValue == null) {
                core.getConfig().set(config.path(), config.defaultValue());
            }

            Config conf = new ConfigImpl(config.path(), config.defaultValue());
            bind(String.class).annotatedWith(conf)
                    .toInstance(core.getConfig().getString(config.path()));
        }

        core.saveConfig();
    }

}
