package me.mykindos.betterpvp.core.world.schematic;

import com.google.inject.Singleton;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PathRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.PolygonRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Captures a player's WorldEdit selection — its blocks <em>and</em> the Mapper data-points standing inside it — into a
 * single {@link Schematic}.
 * <p>
 * Blocks and data-points are read in one operation against one anchor, which is the whole point: saved separately they
 * can be saved at different moments from different positions, and the resulting structure pastes a hull whose helm is
 * somewhere out to sea with nothing to warn the builder.
 *
 * <h3>The anchor</h3>
 * A {@link StructureAnchor#POINT} marker inside the selection is the structure's origin — the spot that lands on whatever
 * marker the structure is later pasted at, and the facing the rotation is measured from. Authoring it deliberately
 * beats using the builder's own position, which makes the alignment depend on where somebody happened to be standing
 * and is invisible until the paste is three blocks out.
 * <p>
 * For a vessel it belongs at the stern, one block above the waterline, facing along the hull: place the berth marker
 * one block above the sea at the destination and the ship floats at the right depth, pointing the right way, with
 * nothing measured by hand.
 * <p>
 * Without one, the builder's position is used and the save says so — a plain decorative build does not need the
 * ceremony.
 * <p>
 * {@code Region} here is Mapper's; WorldEdit's same-named selection type is spelled out where it appears.
 */
@Singleton
@CustomLog
public class StructureCapture {

    /**
     * Captures {@code player}'s current selection.
     *
     * @return the captured structure, or empty if the player has no complete selection
     */
    public @NotNull Optional<Schematic> capture(@NotNull Player player) {
        final World world = player.getWorld();
        final com.sk89q.worldedit.regions.Region selection;
        try {
            selection = WorldEdit.getInstance().getSessionManager()
                    .get(BukkitAdapter.adapt(player))
                    .getSelection(BukkitAdapter.adapt(world));
        } catch (IncompleteRegionException exception) {
            return Optional.empty();
        }

        final List<Region> inside = regionsInside(world,
                point -> selection.contains(BlockVector3.at(point.getBlockX(), point.getBlockY(), point.getBlockZ())));

        // Resolved before anything is measured, because both halves of the structure are stored relative to it.
        final Location anchor = StructureAnchor.find(inside).orElseGet(player::getLocation);

        final List<CapturedRegion> regions = new ArrayList<>(inside.size());
        for (Region region : inside) {
            regions.add(CapturedRegion.capture(region, anchor));
        }

        return Optional.of(captureBlocks(world, selection, anchor).withRegions(regions, anchor.getYaw()));
    }

    private @NotNull Schematic captureBlocks(@NotNull World world,
                                             @NotNull com.sk89q.worldedit.regions.Region selection,
                                             @NotNull Location anchor) {
        final BlockVector3 min = selection.getMinimumPoint();
        final BlockVector3 max = selection.getMaximumPoint();

        final List<Schematic.PlacedBlock> blocks = new ArrayList<>();
        for (BlockVector3 position : selection) {
            blocks.add(new Schematic.PlacedBlock(
                    position.x() - min.x(),
                    position.y() - min.y(),
                    position.z() - min.z(),
                    world.getBlockAt(position.x(), position.y(), position.z()).getBlockData()));
        }

        // The anchor is stored min-relative to match how FaweSchematicFormat reports a clipboard origin, so a structure
        // captured here and one imported from a .schem paste identically.
        return new Schematic(
                max.x() - min.x() + 1, max.y() - min.y() + 1, max.z() - min.z() + 1,
                anchor.getBlockX() - min.x(), anchor.getBlockY() - min.y(), anchor.getBlockZ() - min.z(),
                blocks);
    }

    /**
     * Every data-point lying wholly inside the selection. Wholly, not partly: a hull bounds region half outside the
     * selection would paste a truncated volume, and silently shipping a broken one is worse than leaving the builder to
     * widen their selection.
     */
    private @NotNull List<Region> regionsInside(@NotNull World world, @NotNull Predicate<Location> inSelection) {
        // For editing: a structure is captured in the build world where its data-points live, which is exactly the
        // world content is not allowed to spawn from.
        final var authored = MapperHelper.readRegionsForEditing(world);
        if (authored.isEmpty()) {
            return List.of();
        }

        final List<Region> inside = new ArrayList<>();
        for (Region region : authored.get()) {
            region.setWorld(world);
            if (containsAll(region, inSelection)) {
                inside.add(region);
            }
        }
        return inside;
    }

    private boolean containsAll(@NotNull Region region, @NotNull Predicate<Location> inSelection) {
        for (Location point : pointsOf(region)) {
            if (point == null || !inSelection.test(point)) {
                return false;
            }
        }
        return true;
    }

    /** Every position a region occupies that must be inside the selection for it to be captured whole. */
    private @NotNull List<Location> pointsOf(@NotNull Region region) {
        return switch (region.getType()) {
            case POINT, PERSPECTIVE -> List.of(((PointRegion) region).getLocation());
            case CUBOID -> List.of(((CuboidRegion) region).getMin(), ((CuboidRegion) region).getMax());
            case PATH -> ((PathRegion) region).getPoints();
            case POLYGON -> {
                final List<Location> points = new ArrayList<>();
                for (CuboidRegion child : ((PolygonRegion) region).getChildren()) {
                    points.add(child.getMin());
                    points.add(child.getMax());
                }
                yield points;
            }
        };
    }
}
