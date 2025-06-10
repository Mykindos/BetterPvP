package me.mykindos.betterpvp.core.client.stats;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Builder
@Getter
public class MinecraftStat {
    public static String prefix = "MINECRAFT_";
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

    public static MinecraftStat fromString(@NotNull final String statName) {
        Preconditions.checkArgument(statName.startsWith(prefix), "statName must start with " + prefix);
        final MinecraftStatBuilder builder = MinecraftStat.builder();
        final int extraIndex = statName.lastIndexOf(qualifierSeparator);
        final String typeName = statName.substring(0, extraIndex != -1 ? extraIndex : statName.length());

        final Statistic stat = Statistic.valueOf(typeName);

        builder.statistic(stat);

        if (stat.getType() == Statistic.Type.BLOCK || stat.getType() == Statistic.Type.ITEM) {
            final Material mat = Material.getMaterial(statName.substring(typeName.length() + qualifierSeparator.length()));
            builder.material(mat);
        }

        if (stat.getType() == Statistic.Type.ENTITY) {
            final EntityType ent = EntityType.fromName(statName.substring(typeName.length() + qualifierSeparator.length()));
            builder.entityType(ent);
        }

        return builder.build();
    }

    /**
     * Get the base stat, used by formatters
     * @return
     */
    public String getBaseStat() {
        return prefix + statistic.name();
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

}
