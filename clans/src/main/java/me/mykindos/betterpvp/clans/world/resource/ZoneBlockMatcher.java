package me.mykindos.betterpvp.clans.world.resource;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Matches every block inside a {@link Zone}, whatever its material — the spatial counterpart to the material-keyed
 * matchers, for rules that apply to "anywhere in this node" rather than "this kind of block".
 * <p>
 * Bounded by the zone rather than by the node's Mapper region so it works for every bounds shape a node can carry
 * (a tree's footprint is computed from its schematic, not from a cuboid), and so it agrees exactly with the zone the
 * harvest flow already gates on.
 * <p>
 * {@link #knownMaterials()} answers with every material: this matcher is fully dynamic, and the interface asks such
 * matchers for a strict over-approximation. That only widens registration-time conflict detection, never what
 * {@link #matches(Block)} actually accepts.
 */
@EqualsAndHashCode
public final class ZoneBlockMatcher implements BlockMatcher {

    private static final Set<Material> ALL_MATERIALS =
            Collections.unmodifiableSet(EnumSet.allOf(Material.class));

    private final Zone zone;

    public ZoneBlockMatcher(@NotNull Zone zone) {
        this.zone = zone;
    }

    @Override
    public boolean matches(@NotNull Block block) {
        return zone.contains(block.getLocation());
    }

    @Override
    public @NotNull Set<Material> knownMaterials() {
        return ALL_MATERIALS;
    }
}
