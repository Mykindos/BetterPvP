package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class MinecraftStat implements IBuildableStat {
    //todo convert to parser
    public static String PREFIX = "MINECRAFT_";
    public static String qualifierSeparator = "__";

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

    /**
     * Get the {@link MinecraftStat} represented by this statName
     * @param statName the statName
     * @return the {@link MinecraftStat}
     * @throws IllegalArgumentException if the {@code statName} does not represent a {@link MinecraftStat}
     */
    public static MinecraftStat fromString(@NotNull final String statName) {
        Preconditions.checkArgument(statName.startsWith(PREFIX), "statName must start with " + PREFIX);
        final MinecraftStatBuilder builder = MinecraftStat.builder();
        final int extraIndex = statName.lastIndexOf(qualifierSeparator);
        final String typeName = statName.substring(PREFIX.length(), extraIndex != -1 ? extraIndex : statName.length());

        final Statistic stat = Statistic.valueOf(typeName);

        builder.statistic(stat);

        if (isMaterialStatistic(stat)) {

            try {
                final Material mat = Material.getMaterial(statName.substring(PREFIX.length() + typeName.length() + qualifierSeparator.length()));
                if (mat != null) {
                    builder.material(mat);
                }
            } catch (StringIndexOutOfBoundsException ignored) {
                //out of bounds = no material
            }

        }

        if (isEntityStatistic(stat)) {
            try {
                final EntityType ent = EntityType.fromName(statName.substring(PREFIX.length() + typeName.length() + qualifierSeparator.length()));
                if (ent != null) {
                    builder.entityType(ent);
                }
            } catch (StringIndexOutOfBoundsException ignored) {
                //out of bounds = no entity type
            }

        }

        return builder.build();
    }

    /**
     * Get the base stat, used by formatters
     * @return
     */
    public String getBaseStat() {
        return PREFIX + statistic.name();
    }

    /**
     * Get the full stat, used to store values
     * @return
     * @see MinecraftStat#fromString(String)
     */
    public String getFullStat() {
        if (material != null) {
            return getBaseStat() + qualifierSeparator + material.name();
        }
        if (entityType != null) {
            return getBaseStat() + qualifierSeparator + entityType.name();
        }
        return getBaseStat();
    }

    /**
     * Given the statContainer, return the stat
     * @param statContainer
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        //material composite
        if (isMaterialStatistic(statistic) && material == null) {
            return statContainer.getCompositeMinecraftStat(this, period);
        }
        //entity composite
        if (isEntityStatistic(statistic) && entityType == null) {
            return statContainer.getCompositeMinecraftStat(this, period);
        }
        return statContainer.getProperty(period, getFullStat());
    }

    @Override
    public String getStatName() {
        return getFullStat();
    }

    @Override
    public boolean isSavable() {
        if (!statistic.isSubstatistic()) {
            return true;
        }

        if (isMaterialStatistic(statistic) && material != null) {
            return true;
        }

        if (isEntityStatistic(statistic) && entityType != null) {
            return true;
        }

        return false;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        try {
            final MinecraftStat other = MinecraftStat.fromString(statName);
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

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isMaterialStatistic(Statistic statistic) {
        return statistic.getType() == Statistic.Type.BLOCK || statistic.getType() == Statistic.Type.ITEM;
    }

    private static boolean isEntityStatistic(Statistic statistic) {
        return statistic.getType() == Statistic.Type.ENTITY;
    }

    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        MinecraftStat other = fromString(statName);
        this.statistic = other.getStatistic();
        this.material = other.getMaterial();
        this.entityType = other.getEntityType();
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
