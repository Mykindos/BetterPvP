package me.mykindos.betterpvp.game.framework.manager;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.metadata.MapMetadata;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.global.CurrentMapAttribute;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/**
 * Handles map selection and loading
 */
@CustomLog
@Singleton
public class MapManager {

    private final File mapsFolder;
    private final Set<MappedWorld> availableMaps = new HashSet<>();
    private final Random random = new Random();
    @Getter
    private final MappedWorld waitingLobby;
    private final Provider<CurrentMapAttribute> currentMapAttribute;
    private final ServerController serverController;
    private String previousMap;

    @Inject
    public MapManager(GamePlugin plugin, Provider<CurrentMapAttribute> currentMapAttribute, ServerController serverController) {
        this.currentMapAttribute = currentMapAttribute;
        this.mapsFolder = Compatibility.MINEPLEX ? new File(System.getProperty("user.dir"), "/assets/maps") : new File(plugin.getDataFolder(), "maps");
        this.serverController = serverController;
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }

        final File waitingLobbyZip = new File(mapsFolder, "Waiting Lobby.zip");
        if (!waitingLobbyZip.exists()) {
            log.error("\"Waiting Lobby.zip\" is not present, shutting down...").submit();
            Bukkit.shutdown();
        }

        waitingLobby = getMap(waitingLobbyZip);
        scanForMaps();
    }

    public MappedWorld getCurrentMap() {
        return currentMapAttribute.get().getValue();
    }

    public void setCurrentMap(@NotNull MappedWorld map) {
        final String currentGame = serverController.getCurrentGame().getConfiguration().getName();
        final String mapGameMode = map.getMetadata().getGameMode();
        Preconditions.checkArgument(mapGameMode.equalsIgnoreCase(currentGame), "Map game mode does not match current game mode");
        final MappedWorld old = currentMapAttribute.get().getValue();
        if (old != null) {
            old.unloadWorld();
        }
        map.createWorld();
        currentMapAttribute.get().setValue(map);
        log.info("Selected map {}", map.getName()).submit();
    }

    @SneakyThrows
    public void unload() {
        // So they get deleted
        getCurrentMap().unloadWorld();
        getWaitingLobby().unloadWorld();
    }

    /**
     * Scans the world container for valid maps
     */
    public void scanForMaps() {
        availableMaps.clear();
        File[] files = mapsFolder.listFiles();

        if (files == null) {
            log.warn("No files found in /maps/ container").submit();
            return;
        }

        for (File file : files) {
            if (!file.getName().endsWith(".zip")) {
                continue;
            }

            // Check if this is a valid map (has metadata file and regions)
            final MappedWorld map = getMap(file);
            if (map != null) {
                availableMaps.add(map);
                log.info("Found map: {}", file.getName()).submit();
            }
        }

        log.info("Found {} maps", availableMaps.size()).submit();
    }

    /**
     * Get a map by world folder
     * @param templateZip The world folder
     * @return The map, or null if not found
     */
    private MappedWorld getMap(File templateZip) {
        try (final ZipFile zipFile = new ZipFile(templateZip)) {
            // Read map metadata
            InputStream metadataStream = getStreamFromPath(zipFile, "metadata.json");
            if (metadataStream == null) {
                return null;
            }

            MapMetadata metadata = MapperPlugin.getInstance().getMetadataManager().loadMetadata(metadataStream);
            final JsonExportStrategy json = (JsonExportStrategy) MapperPlugin.getInstance().getExportManager().getAvailableStrategies().get("json");
            if (metadata == null || metadata.getGameMode().isEmpty()) {
                return null;
            }

            // Read regions
            InputStream regionsStream = getStreamFromPath(zipFile, "dataPoints.json");
            if (regionsStream == null) {
                return null;
            }

            List<Region> regions = json.read(regionsStream);
            PerspectiveRegion spawnpoint = regions.stream()
                    .filter(region -> region instanceof PerspectiveRegion)
                    .filter(region -> region.getName().equals("spawnpoint"))
                    .map(region -> (PerspectiveRegion) region)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Map must have a spawnpoint region defined"));

            // Create the map
            final MappedWorld world = new MappedWorld(templateZip, metadata, regions, spawnpoint);

            // Custom bounds
            final Optional<PointRegion> maxBounds = regions.stream()
                    .filter(region -> region instanceof PointRegion)
                    .filter(region -> region.getName().equals("max_bounds"))
                    .map(region -> (PointRegion) region)
                    .findAny();
            final Optional<PointRegion> minBounds = regions.stream()
                    .filter(region -> region instanceof PointRegion)
                    .filter(region -> region.getName().equals("min_bounds"))
                    .map(region -> (PointRegion) region)
                    .findAny();
            if (maxBounds.isPresent() && minBounds.isPresent()) {
                final Location max = maxBounds.get().getLocation();
                final Location min = minBounds.get().getLocation();
                final BoundingBox bounds = new BoundingBox(
                        min.getX(), min.getY(), min.getZ(),
                        max.getX(), max.getY(), max.getZ()
                );

                world.setBounds(bounds);
                world.getMetadata().setMaxHeight((int) Math.ceil(bounds.getMaxY()));

                log.info("Map {} has custom bounds, overwriting max height", world.getName()).submit();
            }

            return world;
        } catch (Exception e) {
            log.error("Failed to load map: {}", templateZip.getName(), e).submit();
            return null;
        }
    }

    /**
     * Selects a random map from available maps
     * 
     * @return Optional containing the selected map, or empty if no maps are available
     */
    public Optional<MappedWorld> selectRandomMap(AbstractGame<?, ?> game) {
        final List<MappedWorld> worlds = availableMaps.stream()
                .filter(map -> map.getMetadata().getGameMode().equalsIgnoreCase(game.getConfiguration().getName()))
                .toList();
        if (worlds.isEmpty()) {
            return Optional.empty();
        }


        //if the new map is equal to the previous map, get a new one unless there is only 1 valid map
        MappedWorld newMap = worlds.get(random.nextInt(worlds.size()));
        while (newMap.getName().equals(previousMap) && worlds.size() > 1) {
            newMap = worlds.get(random.nextInt(worlds.size()));
        }
        previousMap = newMap.getName();

        return Optional.of(newMap);
    }

    public Set<MappedWorld> getAvailableMaps() {
        return Collections.unmodifiableSet(availableMaps);
    }

    private InputStream getStreamFromPath(ZipFile zipFile, String path) {
        try {
            return zipFile.getInputStream(zipFile.getFileHeader(path));
        } catch (Exception e) {
            log.error("Failed to get stream from path: {}", path, e).submit();
            return null;
        }
    }
}
