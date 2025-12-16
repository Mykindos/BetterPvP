package me.mykindos.betterpvp.core.client.stats;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
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
        Reflections reflections = new Reflections(Core.PACKAGE);
        Set<Class<? extends IBuildableStat>> classes = reflections.getSubTypesOf(IBuildableStat.class);
        return classes.stream()
                .filter(clazz -> !clazz.isInterface())
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> !clazz.isEnum())
                .filter(clazz -> !clazz.isAnnotationPresent(Deprecated.class))
                .collect(Collectors.toSet());
    }

    public IStat getStatForStatData(@NotNull String statType, JSONObject data) {
        //todo make builder stats a map based on statType
        for (Class<? extends IBuildableStat> buildableStat : builderStats) {
            IBuildableStat newInstance = null;
            try {
                newInstance = buildableStat.getConstructor().newInstance();
                return newInstance.copyFromStatData(statType, data);
            } catch (IllegalArgumentException | JSONException exception) {
                if (newInstance != null && statType.equals(newInstance.getStatType())) {
                    log.error("Error creating a stat of type {} data {} ", statType, data, exception).submit();
                }
            } catch (Exception e) {
                log.error("Error getting stat for name {} {}", statType, data.toString(), e).submit();
            }
        }
        try {
            return ClientStat.valueOf(statType);
        } catch (IllegalArgumentException ignored) {
        }

        log.warn("No stat found for {} {}", statType, data.toString()).submit();
        return new IStat() {
            @Override
            public Long getStat(StatContainer statContainer, String periodKey) {
                return statContainer.getProperty(getStatType(), this);
            }

            @Override
            public @NotNull String getStatType() {
                return statType;
            }

            /**
             * Get the jsonb data in string format for this object
             *
             * @return
             */
            @Override
            public @Nullable JSONObject getJsonData() {
                return data;
            }

            @Override
            public boolean isSavable() {
                return true;
            }

            @Override
            public boolean containsStat(IStat otherStat) {
                return this.equals(otherStat);
            }

            /**
             * <p>Get the generic stat that includes this stat.</p>
             * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
             *
             * @return the generic stat
             */
            @Override
            public @NotNull IStat getGenericStat() {
                return this;
            }
        };

    }

}
