package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.metadata.MapMetadata;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.util.UtilResource;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Handles map selection and loading
 */
@CustomLog
@Singleton
public class MapManager {

    private final List<MappedWorld> availableMaps = new ArrayList<>();
    private final Random random = new Random();
    private final MapperPlugin mapperPlugin;
    @Getter
    private final MappedWorld waitingLobby;
    @Getter @Setter
    private MappedWorld currentMap;
    
    @Inject
    public MapManager() {
        this.mapperPlugin = MapperPlugin.getInstance();

        // unzip the waiting lobby, so it can be loaded
        try {
            final File file = UtilResource.unzipFile(Bukkit.getWorldContainer(), "Waiting Lobby");
            waitingLobby = new MappedWorld(file);
        } catch (IOException e) {
            log.error("Failed to unzip waiting lobby, shutting down...", e).submit();
            Bukkit.shutdown();
            throw new RuntimeException(e);
        }

        scanForMaps();
    }
    
    /**
     * Scans the world container for valid maps
     */
    public void scanForMaps() {
        availableMaps.clear();
        File worldContainer = Bukkit.getWorldContainer();
        File[] files = worldContainer.listFiles();
        
        if (files == null) {
            log.warn("No files found in world container").submit();
            return;
        }
        
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            
            // Check if this is a valid map (has metadata file and regions)
            final MappedWorld map = getCurrentMap(file);
            if (map != null) {
                availableMaps.add(map);
                log.info("Found map: {}", file.getName()).submit();
            }
        }
        
        log.info("Found {} maps", availableMaps.size()).submit();
    }

    /**
     * Get a map by world folder
     * @param worldFolder The world folder
     * @return The map, or null if not found
     */
    private MappedWorld getCurrentMap(File worldFolder) {
        try {
            // Check for metadata file
            MapMetadata metadata = mapperPlugin.getMetadataManager().loadMetadata(worldFolder);
            if (metadata == null) {
                return null;
            }
            
            // Check for regions file
            File regionsFile = new File(worldFolder, "dataPoints.json");
            if (!regionsFile.exists()) {
                return null;
            }

            return new MappedWorld(worldFolder);
        } catch (Exception e) {
            log.error("Failed to load map: {}", worldFolder.getName(), e).submit();
            return null;
        }
    }
    
    /**
     * Selects a random map from available maps
     * 
     * @return Optional containing the selected map, or empty if no maps are available
     */
    public Optional<MappedWorld> selectRandomMap() {
        if (availableMaps.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(availableMaps.get(random.nextInt(availableMaps.size())));
    }
    
    /**
     * Gets the number of available maps
     * 
     * @return Number of maps
     */
    public int getAvailableMapsCount() {
        return availableMaps.size();
    }
}