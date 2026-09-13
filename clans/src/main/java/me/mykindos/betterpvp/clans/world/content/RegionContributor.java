package me.mykindos.betterpvp.clans.world.content;

import dev.brauw.mapper.region.Region;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Supplies data-points that are not in a world's Mapper file.
 * <p>
 * Everything the content pipeline installs is driven by data-points read off disk, which works right up until something
 * puts a build into the world at runtime, so a ship pasted at a berth brings its helm, its crew NPC and its hull bounds
 * with it, and none of those exist in the world's own file. A contributor hands them over so the props, residents and
 * zones that follow treat them exactly like authored markers.
 * <p>
 * Called once per world load, before content is asked for anything, so a contributor that also has work to do (pasting
 * the blocks the markers describe) can do it here and return what it placed.
 */
@FunctionalInterface
public interface RegionContributor {

    /**
     * @param world    the world being loaded
     * @param authored what the world's own Mapper file declares, since a contributor is usually triggered by a marker in
     *                 here (a berth saying which structure belongs at it), so it is handed over rather than re-read
     * @return the regions to add to this world's index for this load, or empty if this contributor has nothing here
     */
    @NotNull List<Region> contribute(@NotNull World world, @NotNull Collection<Region> authored);
}
