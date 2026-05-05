package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Predicate over blocks. Two responsibilities:
 * <ol>
 *   <li>{@link #matches(Block)} — runtime check while resolving a break.</li>
 *   <li>{@link #knownMaterials()} — best-effort enumeration used to detect
 *       conflicts between rules at registration time (see {@code ToolComponent}).</li>
 * </ol>
 */
public interface BlockMatcher {

    boolean matches(@NotNull Block block);

    /**
     * Materials this matcher is statically known to accept. Used for conflict
     * detection. Should be a strict over-approximation of what {@link #matches(Block)}
     * could return true for; if a matcher is fully dynamic, return all materials.
     */
    @NotNull
    Set<Material> knownMaterials();

    /**
     * @return true when the static {@link #knownMaterials()} sets of this matcher
     * and {@code other} share at least one material.
     */
    default boolean overlaps(@NotNull BlockMatcher other) {
        final Set<Material> mine = knownMaterials();
        final Set<Material> theirs = other.knownMaterials();
        // iterate the smaller side
        if (mine.size() > theirs.size()) {
            for (Material m : theirs) if (mine.contains(m)) return true;
        } else {
            for (Material m : mine) if (theirs.contains(m)) return true;
        }
        return false;
    }
}
