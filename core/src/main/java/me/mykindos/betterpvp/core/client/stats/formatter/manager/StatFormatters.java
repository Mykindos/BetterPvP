package me.mykindos.betterpvp.core.client.stats.formatter.manager;

import com.google.inject.Injector;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.formatter.IStatFormatter;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

@CustomLog
public class StatFormatters {
    private static Map<String, IStatFormatter> formatters = new ConcurrentHashMap<>();

    static {
        loadFormatters();
    }

    public static Description getDescription(String statName, Double value) {
        //special case, this is a Minecraft description
        if (statName.startsWith(MinecraftStat.prefix)) {
            final MinecraftStat minecraftStat = MinecraftStat.fromString(statName);
            IStatFormatter formatter = formatters.get(minecraftStat.getBaseStat());
            if (formatter == null) {
                //get the default Minecraft Stat formatter
                formatter = formatters.get(MinecraftStat.prefix);
            }
            return formatter.getDescription(statName, value);
        }

        IStatFormatter statFormatter = formatters.get(statName);

        if (statFormatter == null) {
            statFormatter = formatters.get("");
        }

        //TODO Minecraft Descriptions

        return statFormatter.getDescription(statName, value);

    }


    private static void loadFormatters() {
        final Core core = JavaPlugin.getPlugin(Core.class);
        final Injector injector = core.getInjector();

        Reflections reflections = new Reflections(core.getPACKAGE());
        Set<Class<? extends IStatFormatter>> classes = reflections.getSubTypesOf(IStatFormatter.class);

        for (Class<? extends IStatFormatter> clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum() || clazz.isAnnotationPresent(Deprecated.class))
                continue;
            final IStatFormatter iStatFormatter = injector.getInstance(clazz);
            formatters.put(iStatFormatter.getStatType(), iStatFormatter);
            log.error("Loaded Formatter: {}", iStatFormatter.getStatType()).submit();
        }

    }

    //todo get all non default formatter keys (for retrieval from db so we can get 0s)
}
