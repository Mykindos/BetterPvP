package me.mykindos.betterpvp.core.client.stats.formatter.manager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.formatter.GenericClientStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.IStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.category.IStatCategory;
import me.mykindos.betterpvp.core.client.stats.formatter.category.SubStatCategory;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@CustomLog
@Singleton
public class StatFormatterLoader {
    private final StatFormatterManager statFormatterManager;

    private final Core core = JavaPlugin.getPlugin(Core.class);


    @Inject
    public StatFormatterLoader(StatFormatterManager statFormatterManager) {
        this.statFormatterManager = statFormatterManager;
    }

    public void loadAll() {
        loadFormatters();
        loadCategories();
    }

    private void loadFormatters() {
        final Injector injector = core.getInjector();

        Reflections reflections = new Reflections(core.getPACKAGE());
        Set<Class<? extends IStatFormatter>> classes = reflections.getSubTypesOf(IStatFormatter.class);

        for (Class<? extends IStatFormatter> clazz : classes) {
            if (clazz.isInterface() ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    clazz.isEnum() ||
                    clazz.isAnnotationPresent(Deprecated.class) ||
                    clazz.equals(GenericClientStatFormatter.class)
            )
                continue;
            log.warn(clazz.getName()).submit();
            final IStatFormatter iStatFormatter = injector.getInstance(clazz);
            if (iStatFormatter.getStatType() != null) {
                statFormatterManager.addObject(iStatFormatter.getStatType(), iStatFormatter);
            }

            log.error("Loaded formatter: {}", iStatFormatter.getStatType()).submit();
        }


    }

    private void loadCategories() {
        final Injector injector = core.getInjector();

        Reflections reflections = new Reflections(core.getPACKAGE());
        Set<Class<? extends IStatCategory>> classes = reflections.getSubTypesOf(IStatCategory.class);

        for (Class<? extends IStatCategory> clazz : classes) {
            if (clazz.isInterface() ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    clazz.isEnum() ||
                    clazz.isAnnotationPresent(Deprecated.class) ||
                    clazz.isAnnotationPresent(SubStatCategory.class)
            )
                continue;
            final IStatCategory statCategory = injector.getInstance(clazz);
            statFormatterManager.getCategories().put(statCategory.getName(), statCategory);
            log.error("Loaded category Formatter: {}", statCategory.getName()).submit();
        }

        Set<Class<?>> children = reflections.getTypesAnnotatedWith(SubStatCategory.class);
        for (Class<?> clazz : children) {
            if (clazz.isInterface() ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    clazz.isEnum() ||
                    clazz.isAnnotationPresent(Deprecated.class) ||
                    !clazz.isAssignableFrom(IStatCategory.class)
            )
                continue;
            final IStatCategory category = (IStatCategory) injector.getInstance(clazz);
            final SubStatCategory annotation = category.getClass().getAnnotation(SubStatCategory.class);

            final IStatCategory parent = injector.getInstance(annotation.value());
            parent.getChildren().add(category);
            statFormatterManager.getCategories().put(category.getName(), category);
            log.error("Loaded category child Formatter: {}", category.getName()).submit();
        }
    }

}
