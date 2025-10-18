package me.mykindos.betterpvp.core.client.stats;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@CustomLog
public class StatBuilder {
    private final Set<Class<? extends IBuildableStat>> builderStats;

    public StatBuilder() {
        this.builderStats = load();
    }

    private Set<Class<? extends IBuildableStat>>  load() {
        Reflections reflections = new Reflections(JavaPlugin.getPlugin(Core.class).getPACKAGE());
        Set<Class<? extends IBuildableStat>> classes = reflections.getSubTypesOf(IBuildableStat.class);
        return classes.stream()
                .filter(clazz -> !clazz.isInterface())
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> !clazz.isEnum())
                .filter(clazz -> !clazz.isAnnotationPresent(Deprecated.class))
                .collect(Collectors.toSet());
    }

    public IStat getStatForStatName(String statName) {
        for (Class<? extends IBuildableStat> buildableStat : builderStats) {
            try {
                return buildableStat.getConstructor().newInstance().copyFromStatname(statName);
            } catch (IllegalArgumentException ignored) {
                continue;
            } catch (Exception e) {
                log.error("Error getting stat for name {} ", statName, e).submit();
            }
        }
        try {
            return ClientStat.valueOf(statName);
        } catch (IllegalArgumentException ignored) {

        }

        log.warn("No stat found for {}", statName).submit();
        return new IStat() {
            @Override
            public Double getStat(StatContainer statContainer, String periodKey) {
                return statContainer.getProperty(getStatName(), this);
            }

            @Override
            public String getStatName() {
                return statName;
            }

            @Override
            public boolean isSavable() {
                return true;
            }

            @Override
            public boolean containsStat(String statName) {
                return getStatName().equals(statName);
            }

            @Override
            public boolean containsStat(IStat otherStat) {
                return containsStat(otherStat.getStatName());
            }
        };

    }

}
