package me.mykindos.betterpvp.core.world.site;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a party is put down when it reaches a world, read from that world's Mapper markers. Which marker name counts
 * is the caller's to say, since a site may be entered more than one way.
 */
@CustomLog
public final class ArrivalPoints {

    /** The marker every world already carries, and the default for any site. */
    public static final String DEFAULT_MARKER = "ship_arrive";

    /** One place a party can be put down, under the name its marker carries. */
    @Value
    public static class Point {
        @NotNull String name;
        @NotNull Location location;
    }

    private ArrivalPoints() {
    }

    /**
     * Every place a party can be set down in a world, in map order.
     */
    public static @NotNull List<Point> in(@NotNull World world, @NotNull String markerName) {
        final List<Point> points = new ArrayList<>();

        MapperHelper.readRegions(world).ifPresent(regions -> {
            for (Region region : regions) {
                if (markerName.equalsIgnoreCase(region.getName()) && region instanceof PerspectiveRegion marker) {
                    marker.setWorld(world);
                    points.add(new Point(RegionTags.of(marker).getString("name", marker.getName()), marker.getLocation()));
                }
            }
        });

        return points;
    }

    /**
     * The single spot a whole party lands at, chosen once so they arrive together. A world with no marker of that name
     * falls back to its spawn point, since arriving somewhere slightly wrong beats being unable to arrive at all.
     */
    public static @NotNull Location choose(@NotNull World world, @NotNull String markerName,
                                           @NotNull ArrivalDistribution distribution) {
        final List<Point> points = in(world, markerName);
        if (points.isEmpty()) {
            log.warn("World '{}' has no '{}' marker - falling back to its spawn point", world.getName(), markerName).submit();
            return world.getSpawnLocation();
        }

        final List<String> names = points.stream().map(Point::getName).toList();
        return points.get(Math.floorMod(distribution.select(names), points.size())).getLocation();
    }
}
