package me.mykindos.betterpvp.hub.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.export.model.RegionCollection;
import dev.brauw.mapper.metadata.MapMetadata;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Singleton
public class HubWorld extends BPvPWorld {

    private final MapMetadata metadata;
    private final PerspectiveRegion spawnpoint;
    private final List<Region> regions;
    private final BoundingBox bounds;

    @Inject
    private HubWorld() {
        super("world");
        final World world = Objects.requireNonNull(getWorld());
        final File worldFolder = world.getWorldFolder();
        final File dataPointsFile = new File(worldFolder, "dataPoints.json");

        // then get properties
        final JsonExportStrategy loadStrategy = (JsonExportStrategy) MapperPlugin.getInstance().getExportManager().getAvailableStrategies().get("json");
        final RegionCollection regions = loadStrategy.read(dataPointsFile);
        this.regions = Collections.unmodifiableList(regions);
        this.metadata = MapperPlugin.getInstance().getMetadataManager().loadMetadata(world);

        // get spawnpoint
        this.spawnpoint = regions.stream()
                .filter(region -> region instanceof PerspectiveRegion)
                .filter(region -> region.getName().equals("spawnpoint"))
                .map(region -> (PerspectiveRegion) region)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Hub world must have a spawnpoint of type PerspectiveRegion defined"));

        // bounding box, to avoid people escaping
        final PointRegion maxBounds = regions.stream()
                .filter(region -> region instanceof PointRegion)
                .filter(region -> region.getName().equals("max_bounds"))
                .map(region -> (PointRegion) region)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Hub world must have a max_bounds of type PointRegion defined"));
        final PointRegion minBounds = regions.stream()
                .filter(region -> region instanceof PointRegion)
                .filter(region -> region.getName().equals("min_bounds"))
                .map(region -> (PointRegion) region)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Hub world must have a min_bounds of type PointRegion defined"));
        final Location max = maxBounds.getLocation();
        final Location min = minBounds.getLocation();
        final BoundingBox bounds = new BoundingBox(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ()
        );

        this.bounds = bounds;

        // Override the max height
        this.metadata.setMaxHeight((int) Math.ceil(bounds.getMaxY()));

        // load the world last
        createWorld();
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
        // Load it
        super.createWorld();

        // Kill all entities
        final World world = Objects.requireNonNull(getWorld());
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }

        // Set the world properties
        regions.forEach(region -> region.setWorld(world));
        world.setClearWeatherDuration(Integer.MAX_VALUE);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.NORMAL);

        var paperConfig = ((CraftWorld) world).getHandle().getLevel().paperConfig();
        paperConfig.chunks.preventMovingIntoUnloadedChunks = true;
    }

    @Override
    public void unloadWorld() {
        throw new UnsupportedOperationException("Hub world cannot be unloaded");
    }

    @Override
    public void createWorld(WorldCreator creator) {
        throw new UnsupportedOperationException("Hub world cannot be created with custom creator");
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
