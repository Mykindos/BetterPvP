package me.mykindos.betterpvp.core.world.construction;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A kind of building that can be raised. Each type is code, supplied by whatever module owns the content, while its
 * numbers come from that module's config.
 */
public interface StructureType {

    @NotNull String getId();

    @NotNull Component getDisplayName();

    /** Which tier it belongs to, for whatever gates tiers. */
    int getTier();

    /** Ids of the structure types that must already stand before this one can be built. */
    @NotNull Set<String> getRequiredStructures();

    /** A tag the build zone it goes in must carry, or null if any build zone will do. */
    @Nullable String getRequiredZoneTag();

    /** The stage chain, first to last. Stage 0 is what building it produces. Never empty. */
    @NotNull List<StructureStage> getStages();

    @NotNull StructureFlags getFlags();

    /** What moving it costs. Free unless the type says otherwise. */
    default @NotNull ResourceCost getMoveCost() {
        return ResourceCost.NONE;
    }

    /** How long moving it takes. Instant unless the type says otherwise. */
    default @NotNull Duration getMoveTime() {
        return Duration.ZERO;
    }

    /** Every upgrade it offers, across all its stages. */
    default @NotNull List<StructureUpgrade> getUpgrades() {
        return List.of();
    }

    default @NotNull Optional<StructureUpgrade> upgrade(@NotNull String id) {
        return getUpgrades().stream().filter(upgrade -> upgrade.getId().equals(id)).findFirst();
    }

    default @NotNull ResourceCost getRepairCost() {
        return ResourceCost.NONE;
    }

    default @NotNull Duration getRepairTime() {
        return Duration.ZERO;
    }

    /** What taking it from nothing to {@code stage} has cost, which a partial refund is a share of. */
    default @NotNull ResourceCost costUpTo(int stage) {
        ResourceCost total = ResourceCost.NONE;
        for (int i = 0; i <= Math.min(stage, getStages().size() - 1); i++) {
            total = total.plus(getStages().get(i).getCost());
        }
        return total;
    }

    default @NotNull StructureStage stage(int index) {
        return getStages().get(Math.clamp(index, 0, getStages().size() - 1));
    }

    default boolean hasStage(int index) {
        return index >= 0 && index < getStages().size();
    }
}
