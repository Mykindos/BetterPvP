package me.mykindos.betterpvp.clans.config;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.implementations.ConfigImpl;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;


/**
 * Why is this code duplicated?
 * Similar to the ClansListenerLoaded.java, reflection does not work in a predictable manner when each plugin is packaged
 * as individual jar files. If both the Core and Clans were bundled in a single jar, this module could be deleted entirely
 */
public class ClansConfigInjectorModule extends AbstractModule {

    private final Clans clans;
    private final String packageName;

    public ClansConfigInjectorModule(Clans clans, String packageName){
        this.clans = clans;
        this.packageName = packageName;
    }

    @Override
    protected void configure() {
        Reflections reflections = new Reflections(packageName, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);
        for (var field : fields) {
            Config config = field.getAnnotation(Config.class);
            if(config == null) continue;

            var existingValue = clans.getConfig().get(config.path());
            if(existingValue == null){
                clans.getConfig().set(config.path(), config.defaultValue());
            }

            Config conf = new ConfigImpl(config.path(), config.defaultValue());

            if(field.getType().isAssignableFrom(String.class)) {
                bind(String.class).annotatedWith(conf)
                        .toInstance(clans.getConfig().getString(config.path()));
            }else if(field.getType().isAssignableFrom(int.class)){
                bind(int.class).annotatedWith(conf)
                        .toInstance(Integer.parseInt(Objects.requireNonNull(clans.getConfig().getString(config.path()))));
            }else if(field.getType().isAssignableFrom(boolean.class)){
                bind(boolean.class).annotatedWith(conf)
                        .toInstance(Boolean.parseBoolean(clans.getConfig().getString(config.path())));
            }

        }

        clans.saveConfig();
    }

}
