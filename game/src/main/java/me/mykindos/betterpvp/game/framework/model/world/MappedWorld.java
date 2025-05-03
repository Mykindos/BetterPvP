package me.mykindos.betterpvp.game.framework.model.world;

import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.ExportStrategy;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.metadata.MapMetadata;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a {@link BPvPWorld} with a {@link MapMetadata} and a list of {@link Region}s
 */
@Getter
@Setter
public class MappedWorld extends BPvPWorld {

    private final File zipFileTemplate;
    private final MapMetadata metadata;
    private final List<Region> regions;
    private final PerspectiveRegion spawnpoint;
    private BoundingBox bounds;

    @SneakyThrows
    public MappedWorld(@NotNull File zipFileTemplate, MapMetadata metadata, List<Region> regions, PerspectiveRegion spawnpoint) {
        super(getZipNameWithoutExtension(zipFileTemplate));
        this.zipFileTemplate = zipFileTemplate;
        this.metadata = metadata;
        this.regions = regions;
        this.spawnpoint = spawnpoint;
    }

    public static String getZipNameWithoutExtension(File zipFile) {
        String name = zipFile.getName();
        if (name.toLowerCase().endsWith(".zip")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    @SneakyThrows
    @Override
    public void createWorld() {
        // Extract the world to the destination
        final File destination = new File(Bukkit.getWorldContainer(), getName());
        if (destination.exists()) {
            FileUtils.deleteDirectory(destination);
        }

        // Unzip the map
        try (ZipFile zip = new ZipFile(zipFileTemplate)) {
            zip.extractAll(destination.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract world zip file", e);
        }

        // Load it
        super.createWorld();

        // Set the world properties
        final World world = Objects.requireNonNull(getWorld());
        for (Entity entity : world.getEntities()) {
            entity.remove();
        }
        regions.forEach(region -> region.setWorld(world));
        world.setClearWeatherDuration(Integer.MAX_VALUE);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.HARD);
    }

    @SneakyThrows
    @Override
    public void unloadWorld() {
        // Unload the world
        super.unloadWorld();

        // Delete the world
        FileUtils.deleteDirectory(getWorldFolder());
    }

    @Override
    public void createWorld(WorldCreator creator) {
        throw new UnsupportedOperationException();
    }

    public <T extends Region> Stream<T> findRegion(String name, Class<T> type) {
        return regions.stream()
                .filter(region -> region.getName().trim().equals(name))
                .filter(type::isInstance)
                .map(type::cast);
    }

    public <T extends Region> Stream<T> regexRegion(String regex, Class<T> type) {
        return regions.stream()
                .filter(region -> region.getName().trim().matches(regex))
                .filter(type::isInstance)
                .map(type::cast);
    }

    public Optional<BoundingBox> getBoundingBox() {
        return Optional.ofNullable(bounds);
    }

    public boolean isInsideBoundingBox(Location location) {
        if (bounds == null) {
            return location.getY() <= getMetadata().getMaxHeight();
        }

        return bounds.contains(location.getX(), location.getY(), location.getZ());
    }
}