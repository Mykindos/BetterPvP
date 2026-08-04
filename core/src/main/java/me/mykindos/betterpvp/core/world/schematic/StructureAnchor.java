package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * A structure's authored origin: the spot that lands on whatever marker it is pasted at, and the facing its rotation is
 * measured from.
 * <p>
 * Declaring it beats using the capturing player's position, which makes a structure's alignment depend on where
 * somebody happened to be standing and is invisible until a paste is several blocks out.
 * <p>
 * For a vessel it belongs at the stern, one block above the waterline, facing along the hull. Put the berth marker one
 * block above the sea at the destination and the ship floats at the depth it was built at, pointing the right way,
 * with nothing measured by hand.
 * <p>
 * Kept apart from {@link StructureCapture} so the rules can be read — and tested — without dragging in WorldEdit.
 */
@UtilityClass
public class StructureAnchor {

    /** The perspective marker that defines a structure's origin and facing. */
    public static final String POINT = "structure_anchor";

    /**
     * Finds the authored origin among a structure's data-points.
     * <p>
     * A plain point is not accepted: rotation is measured from the anchor's facing, and a point has none — so one
     * authored with the wrong wand would silently paste every copy unrotated.
     *
     * @return the anchor's location, or empty if the structure does not declare one
     */
    public static @NotNull Optional<Location> find(@NotNull List<Region> regions) {
        return regions.stream()
                .filter(region -> POINT.equalsIgnoreCase(region.getName()))
                .filter(PerspectiveRegion.class::isInstance)
                .map(region -> ((PerspectiveRegion) region).getLocation())
                .findFirst();
    }

    /** Whether {@code schematic} was aligned to an authored anchor rather than to whoever captured it. */
    public static boolean isAnchored(@NotNull Schematic schematic) {
        return schematic.getRegions().stream().anyMatch(region -> POINT.equalsIgnoreCase(region.getName()));
    }
}
