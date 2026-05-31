package me.mykindos.betterpvp.core.utilities;

import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.export.model.RegionCollection;
import dev.brauw.mapper.region.Region;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class MapperHelper {

    private MapperHelper() {
    }

    public static RegionCollection getRegions(@NotNull World world) {
        final File dataPointsFile = new File(world.getWorldFolder(), "dataPoints.json");
        final JsonExportStrategy loadStrategy = (JsonExportStrategy) MapperPlugin.getInstance()
                .getExportManager()
                .getAvailableStrategies()
                .get("json");
        return loadStrategy.read(dataPointsFile);
    }

    /**
     * Finds the first region with the given name (case-insensitive) that is an instance of {@code type}.
     *
     * @param regions the regions to search
     * @param name    the data-point name (case-insensitive)
     * @param type    the expected region type
     * @param <T>     the region subtype
     * @return the matching region, if any
     */
    public static <T extends Region> Optional<T> findRegion(@NotNull Collection<? extends Region> regions,
                                                            @NotNull String name, @NotNull Class<T> type) {
        return regions.stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    /**
     * Finds all regions with the given name (case-insensitive) that are instances of {@code type}.
     *
     * @param regions the regions to search
     * @param name    the data-point name (case-insensitive)
     * @param type    the expected region type
     * @param <T>     the region subtype
     * @return an unmodifiable list of matching regions (may be empty)
     */
    public static <T extends Region> List<T> findRegions(@NotNull Collection<? extends Region> regions,
                                                         @NotNull String name, @NotNull Class<T> type) {
        return regions.stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
