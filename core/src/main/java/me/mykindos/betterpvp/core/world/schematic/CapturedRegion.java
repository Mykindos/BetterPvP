package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PathRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.PolygonRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.region.RegionOptions;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One Mapper data-point captured into a {@link Schematic}, stored in the same anchor-relative space as the blocks.
 * <p>
 * Every region type is reduced to an ordered list of points, so rotation and translation are applied once rather than
 * per type — a cuboid is its two corners, a path is its waypoints, a polygon is its children's corners in pairs. Only
 * rebuilding needs to know the difference again.
 */
@Value
@Builder
@Jacksonized
public class CapturedRegion {

    String name;
    Region.RegionType type;
    Set<String> tags;
    List<RelativePoint> points;

    /**
     * Captures {@code region} relative to {@code anchor}.
     * <p>
     * The anchor's <em>block</em> position is the origin, matching {@link SchematicAnimator}'s use of
     * {@code getBlockX()} — the two must agree or the datapoints drift from the blocks they mark.
     */
    public static @NotNull CapturedRegion capture(@NotNull Region region, @NotNull Location anchor) {
        final RegionOptions options = region.getOptions();
        final Set<String> tags = options == null || options.getTags() == null
                ? Set.of()
                : new LinkedHashSet<>(options.getTags());

        return CapturedRegion.builder()
                .name(region.getName())
                .type(region.getType())
                .tags(tags)
                .points(relativePoints(region, anchor))
                .build();
    }

    /**
     * Rebuilds this region in the world, with {@code at} as the anchor and the structure turned {@code quarterTurns}
     * times.
     *
     * @return a live Mapper region, positioned and rotated to match the pasted blocks
     */
    public @NotNull Region rebuild(@NotNull Location at, int quarterTurns) {
        return rebuild(at, quarterTurns, Set.of());
    }

    /**
     * As {@link #rebuild(Location, int)}, with {@code extraTags} added to the rebuilt region.
     * <p>
     * This is how a paste marks what it belongs to. Two copies of one structure in a world hold identical markers, so
     * without something naming the placement, the helm on one is indistinguishable from the helm on the other.
     */
    public @NotNull Region rebuild(@NotNull Location at, int quarterTurns, @NotNull Set<String> extraTags) {
        final List<Location> placed = new ArrayList<>(points.size());
        for (RelativePoint point : points) {
            placed.add(point.toWorld(at, quarterTurns));
        }

        final Set<String> allTags = new LinkedHashSet<>(tags);
        allTags.addAll(extraTags);
        final RegionOptions options = RegionOptions.builder().tags(allTags).build();
        return switch (type) {
            case POINT -> new PointRegion(name, placed.getFirst(), options);
            case PERSPECTIVE -> new PerspectiveRegion(name, placed.getFirst(), options);
            case CUBOID -> new CuboidRegion(name, placed.get(0), placed.get(1), options);
            case PATH -> new PathRegion(name, placed, options);
            case POLYGON -> new PolygonRegion(name, pairIntoCuboids(name, placed), options);
        };
    }

    private static @NotNull List<RelativePoint> relativePoints(@NotNull Region region, @NotNull Location anchor) {
        final List<RelativePoint> points = new ArrayList<>();
        switch (region.getType()) {
            case POINT, PERSPECTIVE -> points.add(RelativePoint.of(((PointRegion) region).getLocation(), anchor));
            case CUBOID -> {
                final CuboidRegion cuboid = (CuboidRegion) region;
                points.add(RelativePoint.of(cuboid.getMin(), anchor));
                points.add(RelativePoint.of(cuboid.getMax(), anchor));
            }
            case PATH -> ((PathRegion) region).getPoints().forEach(point -> points.add(RelativePoint.of(point, anchor)));
            case POLYGON -> {
                for (CuboidRegion child : ((PolygonRegion) region).getChildren()) {
                    points.add(RelativePoint.of(child.getMin(), anchor));
                    points.add(RelativePoint.of(child.getMax(), anchor));
                }
            }
        }
        return List.copyOf(points);
    }

    /** A polygon is stored as its children's corners in min/max pairs; this puts them back together. */
    private static @NotNull List<CuboidRegion> pairIntoCuboids(@NotNull String name, @NotNull List<Location> placed) {
        final List<CuboidRegion> children = new ArrayList<>(placed.size() / 2);
        for (int i = 0; i + 1 < placed.size(); i += 2) {
            children.add(new CuboidRegion(name, placed.get(i), placed.get(i + 1)));
        }
        return children;
    }

    /**
     * A position in the structure's own space: offset from the anchor block, with the facing it was captured with.
     */
    @Value
    @Builder
    @Jacksonized
    public static class RelativePoint {

        double x;
        double y;
        double z;
        float yaw;
        float pitch;

        public static @NotNull RelativePoint of(@NotNull Location location, @NotNull Location anchor) {
            return RelativePoint.builder()
                    .x(location.getX() - anchor.getBlockX())
                    .y(location.getY() - anchor.getBlockY())
                    .z(location.getZ() - anchor.getBlockZ())
                    .yaw(location.getYaw())
                    .pitch(location.getPitch())
                    .build();
        }

        public @NotNull Location toWorld(@NotNull Location at, int quarterTurns) {
            final double[] rotated = StructureTransform.rotateXZ(x, z, quarterTurns);
            return new Location(
                    at.getWorld(),
                    at.getBlockX() + rotated[0],
                    at.getBlockY() + y,
                    at.getBlockZ() + rotated[1],
                    StructureTransform.rotateYaw(yaw, quarterTurns),
                    pitch);
        }
    }
}
