package me.mykindos.betterpvp.clans.world.camp.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import lombok.Value;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStorage;
import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the chests carrying a Mapper point of one name sit in a structure's build as it stands, each with the storage
 * slot it is kept under. Worked out once for each stage and position of a structure.
 */
@Singleton
public class ChestPoints {

    private final StructureShapes shapes;
    private final Map<String, List<Point>> found = new ConcurrentHashMap<>();

    @Inject
    public ChestPoints(@NotNull StructureShapes shapes) {
        this.shapes = shapes;
    }

    /** The chests marked {@code marker} in {@code structure}'s build, in the order the build lists its points. */
    public @NotNull List<CampChest> find(@NotNull World world, @NotNull PlacedStructure structure,
                                         @NotNull String marker) {
        final String key = world.getName() + ":" + structure.getId() + ":" + structure.getStage() + ":"
                + structure.getPosition() + ":" + marker;
        final List<Point> points = found.computeIfAbsent(key, unused -> points(world, structure, marker));
        final List<CampChest> chests = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            final Point point = points.get(i);
            chests.add(new CampChest(structure, i + 1, point.getX(), point.getY(), point.getZ(), point.getSlot()));
        }
        return chests;
    }

    private @NotNull List<Point> points(@NotNull World world, @NotNull PlacedStructure structure,
                                        @NotNull String marker) {
        final List<Point> points = new ArrayList<>();
        shapes.placementOf(world, structure).ifPresent(placement -> {
            final List<StructureStorage.Slot> slots = StructureStorage.slots(placement,
                    LayerPlan.of(placement.getSchematic()));
            for (Region region : placement.markers()) {
                if (marker.equalsIgnoreCase(region.getName()) && region instanceof PointRegion point) {
                    final int x = point.getLocation().getBlockX();
                    final int y = point.getLocation().getBlockY();
                    final int z = point.getLocation().getBlockZ();
                    final StructureStorage.Slot slot = slots.stream()
                            .filter(found -> found.getX() == x && found.getY() == y && found.getZ() == z)
                            .findFirst()
                            .orElse(null);
                    points.add(new Point(x, y, z, slot));
                }
            }
        });
        return List.copyOf(points);
    }

    @Value
    private static class Point {
        int x;
        int y;
        int z;
        @Nullable StructureStorage.Slot slot;
    }
}
