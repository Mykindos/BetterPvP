package me.mykindos.betterpvp.core.utilities;

import dev.brauw.mapper.Mapper;
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

    /**
     * The game mode a builder sets on a map that is still being made. See {@link #isBuildWorld(World)}.
     */
    public static final String BUILD_GAME_MODE = "Build";

    /**
     * Whether this world is being built rather than played, from its Mapper {@code metadata.json}.
     * <p>
     * A build world's data-points are a work in progress: half-placed markers, a resident with no route yet, a berth
     * naming a structure that has not been captured. Spawning content from them puts NPCs and hulls in the way of the
     * person authoring them, and every content system would otherwise have to learn that on its own.
     */
    public static boolean isBuildWorld(@NotNull World world) {
        try {
            return BUILD_GAME_MODE.equalsIgnoreCase(Mapper.get().getMetadataManager().loadMetadata(world).getGameMode());
        } catch (IllegalArgumentException exception) {
            // No metadata file at all - an ordinary unmapped world, not a build one.
            return false;
        }
    }

    /**
     * A world's regions, or nothing at all if it {@link #isBuildWorld(World) is being built}.
     */
    public static RegionCollection getRegions(@NotNull World world) {
        if (isBuildWorld(world)) {
            return new RegionCollection();
        }

        final Mapper mapper = Mapper.get();
        // Asking the storage manager rather than assuming <world>/dataPoints.json keeps us on whatever
        // location Mapper is configured to use, which is no longer fixed.
        final File dataPointsFile = mapper.getStorageManager().getRegionsFile(world);
        final JsonExportStrategy loadStrategy = (JsonExportStrategy) mapper.getExportManager()
                .getAvailableStrategies()
                .get("json");
        return loadStrategy.read(dataPointsFile);
    }

    /**
     * Reads a world's regions only if it has any authored at all.
     * <p>
     * Most worlds on the server are not mapped - survival worlds, freshly cloned instances that carry no data-points -
     * and asking for regions there is a normal outcome, not a failure. Use this over {@link #getRegions(World)} when
     * scanning worlds generically, so an unmapped world reads as "nothing here" rather than an error.
     *
     * A world that {@link #isBuildWorld(World) is being built} also reads as empty, so nothing is spawned into a map
     * somebody is still authoring.
     *
     * @return the world's regions, or empty if it has no data-points file
     */
    public static Optional<RegionCollection> readRegions(@NotNull World world) {
        return isBuildWorld(world) ? Optional.empty() : readRegionsForEditing(world);
    }

    /**
     * A world's regions whether or not it {@link #isBuildWorld(World) is being built} — for builder tooling, which has
     * to see the markers precisely in the world where nothing is allowed to spawn from them. Capturing a
     * {@code .structure} is the case this exists for: it happens in a build world by definition, and a capture that
     * silently dropped the build's data-points would produce a hull with no helm.
     */
    public static Optional<RegionCollection> readRegionsForEditing(@NotNull World world) {
        final Mapper mapper = Mapper.get();
        final File dataPointsFile = mapper.getStorageManager().getRegionsFile(world);
        if (dataPointsFile == null || !dataPointsFile.isFile()) {
            return Optional.empty();
        }

        final JsonExportStrategy loadStrategy = (JsonExportStrategy) mapper.getExportManager()
                .getAvailableStrategies()
                .get("json");
        return Optional.ofNullable(loadStrategy.read(dataPointsFile));
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
