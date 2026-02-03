package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class MinecraftStat implements IBuildableStat {
    public static final String TYPE = "MINECRAFT";

    /**
     * The {@link Statistic} this {@link MinecraftStat} represents
     */
    @NotNull
    private Statistic statistic;
    /**
     * The {@link Material} qualifier of this {@link MinecraftStat}
     */
    @Nullable
    private Material material;
    /**
     * The {@link EntityType} qualifier of this {@link MinecraftStat}
     */
    @Nullable
    private EntityType entityType;

    /**
     * Get the Minecraft Stat represented by this event
     * @param event the {@link PlayerStatisticIncrementEvent}
     * @return the {@link MinecraftStat}
     */
    public static MinecraftStat fromEvent(@NotNull final PlayerStatisticIncrementEvent event) {
        MinecraftStatBuilder builder = MinecraftStat.builder()
                .statistic(event.getStatistic());
        final Material material = event.getMaterial();
        final EntityType entityType = event.getEntityType();
        if (material != null) {
            builder.material(material);
        } else if (entityType != null) {
            builder.entityType(entityType);
        }
        return builder.build();
    }


    public static MinecraftStat fromData(@NotNull final String statType, JSONObject data) {
        final MinecraftStatBuilder builder = MinecraftStat.builder();
        Preconditions.checkArgument(statType.startsWith(TYPE), "statName must start with " + TYPE);
        builder.statistic(Statistic.valueOf(data.getString("statistic")));
        String dataMaterial = data.optString("material", null);
        if (!Strings.isNullOrEmpty(dataMaterial)) {
            builder.material(Material.valueOf(dataMaterial));
        }
        String dataEntity = data.optString("entityType", null);
        if (!Strings.isNullOrEmpty(dataEntity)) {
            builder.entityType(EntityType.valueOf(dataEntity));
        }
        return builder.build();
    }

    /**
     * Get the base stat, used by formatters
     * @return
     */
    public String getBaseStat() {
        return TYPE + statistic.name();
    }

    /**
     * Given the statContainer, return the stat
     * @param statContainer
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        //material composite
        if (isMaterialStatistic(statistic) && material == null) {
            return getFilteredStat(statContainer, type, period, this::filterMinecraftStat);
        }
        //entity composite
        if (isEntityStatistic(statistic) && entityType == null) {
            return getFilteredStat(statContainer, type, period, this::filterMinecraftStat);
        }
        return statContainer.getProperty(type, period, this);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public StatValueType getStatValueType() {
        return StatValueType.LONG;
    }

    boolean filterMinecraftStat(Map.Entry<IStat, Long> entry) {
        MinecraftStat other = (MinecraftStat) entry.getKey();
        return statistic.equals(other.statistic);
    }

    @Override
    public @NotNull String getStatType() {
        return TYPE;
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOnce("statistic", statistic.name())
                .putOpt("material", material)
                .putOpt("entityType", entityType);
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        final StringBuilder stringBuilder = new StringBuilder(UtilFormat.cleanString(statistic.name()));
        if (material != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(material.name()));
        }
        if (entityType != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(entityType.name()));
        }
        return stringBuilder.toString();
    }


    @Override
    public boolean isSavable() {
        if (!statistic.isSubstatistic()) {
            return true;
        }

        if (isMaterialStatistic(statistic) && material != null) {
            return true;
        }

        return isEntityStatistic(statistic) && entityType != null;
    }


    @Override
    public boolean containsStat(IStat stat) {
        if (!(stat instanceof MinecraftStat other)) return false;
        //this is a qualified material statistic, check to make sure it is exact
        if (isMaterialStatistic(statistic) && material != null) {
            return statistic.equals(other.getStatistic()) && material.equals(other.getMaterial());
        }

        //this is a qualified entityType statistic, check to make sure it is exact
        if (isEntityStatistic(statistic) && entityType != null) {
            return statistic.equals(other.getStatistic()) && entityType.equals(other.getEntityType());
        }

        //this is a generic or a non-qualified statistic, therefore check if the base statistic is equal
        return statistic.equals(other.getStatistic());
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return MinecraftStat.builder().statistic(statistic).build();
    }

    private static boolean isMaterialStatistic(Statistic statistic) {
        return statistic.getType() == Statistic.Type.BLOCK || statistic.getType() == Statistic.Type.ITEM;
    }

    private static boolean isEntityStatistic(Statistic statistic) {
        return statistic.getType() == Statistic.Type.ENTITY;
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        MinecraftStat other = fromData(statType, data);
        this.statistic = other.getStatistic();
        this.material = other.getMaterial();
        this.entityType = other.getEntityType();
        return this;
    }
}
