package me.mykindos.betterpvp.core.client.stats.formatter.manager;

import com.google.inject.Injector;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.formatter.IStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.category.IStatCategory;
import me.mykindos.betterpvp.core.client.stats.formatter.category.SubStatCategory;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CustomLog
public class StatFormatters {
    private static final Map<String, IStatFormatter> formatters = new ConcurrentHashMap<>();
    private static final Map<String, IStatCategory> categories = new ConcurrentHashMap<>();

    static {
        loadCategories();
        loadFormatters();
    }

    /**
     *
     * @param statName
     * @return the {@link KeyValue} of <{@code statName}, {@link IStatFormatter}>
     */
    public static KeyValue<String, IStatFormatter> getStatFormatter(String statName) {
        //special case, this is a Minecraft description
        if (statName.startsWith(MinecraftStat.prefix)) {
            final MinecraftStat minecraftStat = MinecraftStat.fromString(statName);
            IStatFormatter formatter = formatters.get(minecraftStat.getBaseStat());
            if (formatter == null) {
                //get the default Minecraft Stat formatter
                formatter = formatters.get(MinecraftStat.prefix);
            }
            return new KeyValue<>(statName, formatter);
        }

        IStatFormatter statFormatter = formatters.get(statName);

        if (statFormatter == null) {
            statFormatter = formatters.get("");
        }

        return new KeyValue<>(statName, statFormatter);

    }

    public static Description getCategoryDescription(String categoryName) {
        return categories.get(categoryName).getDescription();
    }

    public static Collection<IStatCategory> getRootCategories() {
        return categories.values().stream()
                .filter(iStatCategory -> !iStatCategory.getClass().isAnnotationPresent(SubStatCategory.class))
                .collect(Collectors.toSet());
    }


    private static void loadFormatters() {
        final Core core = JavaPlugin.getPlugin(Core.class);
        final Injector injector = core.getInjector();

        Reflections reflections = new Reflections(core.getPACKAGE());
        Set<Class<? extends IStatFormatter>> classes = reflections.getSubTypesOf(IStatFormatter.class);

        for (Class<? extends IStatFormatter> clazz : classes) {
            if (clazz.isInterface() ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    clazz.isEnum() ||
                    clazz.isAnnotationPresent(Deprecated.class)
            )
                continue;
            final IStatFormatter iStatFormatter = injector.getInstance(clazz);
            formatters.put(iStatFormatter.getStatType(), iStatFormatter);
            log.error("Loaded formatter: {}", iStatFormatter.getStatType()).submit();
        }


    }

    private static void loadCategories() {
        final Core core = JavaPlugin.getPlugin(Core.class);
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
            categories.put(statCategory.getName(), statCategory);
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
            categories.put(category.getName(), category);
            log.error("Loaded category child Formatter: {}", category.getName()).submit();
        }
    }

    //todo get all non default formatter keys (for retrieval from db so we can get 0s)
}
