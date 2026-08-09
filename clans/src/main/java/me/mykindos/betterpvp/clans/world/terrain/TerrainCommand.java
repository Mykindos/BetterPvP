package me.mykindos.betterpvp.clans.world.terrain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.export.model.RegionCollection;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.PolygonRegion;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapZoneStyleRegistry;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.terrain.TerrainClassifier;
import me.mykindos.betterpvp.core.world.terrain.TerrainMask;
import me.mykindos.betterpvp.core.world.terrain.TerrainSampler;
import me.mykindos.betterpvp.core.world.terrain.TerrainScanParameters;
import me.mykindos.betterpvp.core.world.terrain.TerrainType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * World-agnostic terrain zoning tooling. {@code /terrain scan [key=value…]} classifies the world the player is standing
 * in into ocean, beach, mountain and interior from a handful of Mapper markers and installs the resulting zones;
 * {@code /terrain info} reports the last scan.
 * <p>
 * Scan settings come from the per-world config profile ({@link TerrainConfig}); any setting can be overridden for a
 * single run with a {@code key=value} flag, e.g. {@code /terrain scan seaLevel=48 mountainBaseY=100}.
 */
@Singleton
public class TerrainCommand extends Command {

    /** Mapper data-point that seeds the ocean flood-fill (a point in open water). */
    private static final String OCEAN_SEED = "ocean_seed";
    /** Mapper data-point that seeds the mountain flood-fill (a point on the summit). */
    private static final String MOUNTAIN_PEAK = "mountain_peak";
    /** Optional Mapper cuboid bounding the scan; falls back to the world border when absent. */
    private static final String SCAN_BOUNDS = "scan_bounds";

    @Override
    public String getName() {
        return "terrain";
    }

    @Override
    public String getDescription() {
        return "Manage world terrain zoning (scan/info)";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Clans",
                Component.text("Usage: /terrain <scan|info>", NamedTextColor.YELLOW));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(TerrainCommand.class)
    private static class ScanSubCommand extends Command {

        @Inject
        private Clans clans;
        @Inject
        private WorldContentService worldContentService;
        @Inject
        private MapZoneStyleRegistry styleRegistry;

        @Override
        public String getName() {
            return "scan";
        }

        @Override
        public String getDescription() {
            return "Scan the current world's terrain and install its zones";
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            final World world = player.getWorld();
            final ExtendedYamlConfiguration config = clans.getConfig();
            final String name = world.getName();
            final Map<String, String> flags = parseFlags(args);

            final TerrainScanParameters parameters = new TerrainScanParameters(
                    flagInt(flags, "seaLevel", TerrainConfig.getInt(config, name, "seaLevel", 62)),
                    flagInt(flags, "beachMinWidth", TerrainConfig.getInt(config, name, "beachMinWidth", 15)),
                    flagInt(flags, "beachMaxWidth", TerrainConfig.getInt(config, name, "beachMaxWidth", 30)),
                    flagInt(flags, "mountainBaseY", TerrainConfig.getInt(config, name, "mountainBaseY", 86)),
                    TerrainConfig.parseMaterials(flags.getOrDefault("beachmaterials",
                            TerrainConfig.getString(config, name, "beachMaterials", "SAND,RED_SAND,GRAVEL,SANDSTONE"))));
            final int maxScanChunks = flagInt(flags, "maxScanChunks", TerrainConfig.maxScanChunks(config, name));

            final RegionCollection authored = MapperHelper.readRegionsForEditing(world).orElseGet(RegionCollection::new);
            final RegionIndex index = RegionIndex.of(world, authored);
            final List<int[]> oceanSeeds = seeds(index, OCEAN_SEED);
            final List<int[]> mountainSeeds = seeds(index, MOUNTAIN_PEAK);
            if (mountainSeeds.isEmpty()) {
                UtilMessage.message(player, "Clans", Component.text(
                        "No '" + MOUNTAIN_PEAK + "' marker found — the mountain won't be classified.", NamedTextColor.YELLOW));
            }

            final int[] bounds = resolveBounds(world, index);
            final long chunks = ((long) ((bounds[2] >> 4) - (bounds[0] >> 4) + 1))
                    * ((bounds[3] >> 4) - (bounds[1] >> 4) + 1);
            if (chunks > maxScanChunks) {
                UtilMessage.message(player, "Clans", Component.text(
                        "Scan area is " + chunks + " chunks (limit " + maxScanChunks + "). Add a '" + SCAN_BOUNDS
                                + "' cuboid around the island or raise the maxScanChunks setting.", NamedTextColor.RED));
                return;
            }

            UtilMessage.message(player, "Clans", Component.text(
                    "Scanning " + chunks + " chunks in '" + name + "'…", NamedTextColor.YELLOW));

            TerrainSampler.sample(world, bounds[0], bounds[1], bounds[2], bounds[3], parameters)
                    .thenAccept(samples -> UtilServer.runTaskAsync(clans, () -> {
                        try {
                            final TerrainMask mask = new TerrainClassifier()
                                    .classify(world, samples, parameters, oceanSeeds, mountainSeeds);
                            applyOverrides(mask, index);
                            UtilServer.runTask(clans, () -> finish(player, world, mask));
                        } catch (Exception exception) {
                            UtilServer.runTask(clans, () -> UtilMessage.message(player, "Clans",
                                    Component.text("Terrain scan failed: " + exception.getMessage(), NamedTextColor.RED)));
                        }
                    }))
                    .exceptionally(throwable -> {
                        UtilServer.runTask(clans, () -> UtilMessage.message(player, "Clans", Component.text(
                                "Terrain scan failed while loading chunks: " + throwable.getMessage(), NamedTextColor.RED)));
                        return null;
                    });
        }

        private void finish(Player player, World world, TerrainMask mask) {
            try {
                mask.save(new File(world.getWorldFolder(), TerrainZones.MASK_FILE_NAME));
            } catch (IOException exception) {
                UtilMessage.message(player, "Clans",
                        Component.text("Failed to save terrain mask: " + exception.getMessage(), NamedTextColor.RED));
                return;
            }

            worldContentService.loadWorld(world);
            styleRegistry.invalidateResolution(); // force the map to re-resolve zone tints against the new zones
            UtilMessage.message(player, "Clans", Component.text("Scan complete: "
                    + mask.coveredChunks(TerrainType.OCEAN).size() + " ocean, "
                    + mask.coveredChunks(TerrainType.BEACH).size() + " beach, "
                    + mask.coveredChunks(TerrainType.MOUNTAIN).size() + " mountain chunks.", NamedTextColor.GREEN));
            if (MapperHelper.isBuildWorld(world)) {
                UtilMessage.message(player, "Clans", Component.text(
                        "This world is in Build mode, so its zones stay inactive until it is set playable.", NamedTextColor.GRAY));
            }
        }

        private int[] resolveBounds(World world, RegionIndex index) {
            return index.findOne(SCAN_BOUNDS, CuboidRegion.class)
                    .map(region -> new int[]{region.getMin().getBlockX(), region.getMin().getBlockZ(),
                            region.getMax().getBlockX(), region.getMax().getBlockZ()})
                    .orElseGet(() -> {
                        final WorldBorder border = world.getWorldBorder();
                        final Location centre = border.getCenter();
                        final int half = (int) Math.ceil(border.getSize() / 2.0);
                        return new int[]{centre.getBlockX() - half, centre.getBlockZ() - half,
                                centre.getBlockX() + half, centre.getBlockZ() + half};
                    });
        }

        /**
         * Docks are deliberately not excluded here. A pier is a structure standing <em>in</em> the sea, and the water
         * beneath it is the same open ocean as the water beside it: blanking its footprint to interior made swimming
         * under the pier safe and left a hole in the ocean ring. What a dock needs — safety, no building, no claiming —
         * comes from its own zone, which outranks the terrain zones. Use {@code force_interior} to opt a specific area
         * out.
         */
        private void applyOverrides(TerrainMask mask, RegionIndex index) {
            // Positive overrides first, then the interior exclusion so it wins where they overlap.
            overlay(mask, index, "force_ocean", TerrainType.OCEAN);
            overlay(mask, index, "force_beach", TerrainType.BEACH);
            overlay(mask, index, "force_mountain", TerrainType.MOUNTAIN);
            overlay(mask, index, "force_interior", TerrainType.INTERIOR);
        }

        private void overlay(TerrainMask mask, RegionIndex index, String name, TerrainType type) {
            for (CuboidRegion cuboid : index.find(name, CuboidRegion.class)) {
                fill(mask, cuboid, type);
            }
            for (PolygonRegion polygon : index.find(name, PolygonRegion.class)) {
                for (CuboidRegion child : polygon.getChildren()) {
                    fill(mask, child, type);
                }
            }
        }

        private void fill(TerrainMask mask, CuboidRegion cuboid, TerrainType type) {
            for (int x = cuboid.getMin().getBlockX(); x <= cuboid.getMax().getBlockX(); x++) {
                for (int z = cuboid.getMin().getBlockZ(); z <= cuboid.getMax().getBlockZ(); z++) {
                    mask.set(x, z, type);
                }
            }
        }

        private List<int[]> seeds(RegionIndex index, String name) {
            return index.find(name, PointRegion.class).stream()
                    .map(point -> new int[]{point.getLocation().getBlockX(), point.getLocation().getBlockZ()})
                    .toList();
        }

        private Map<String, String> parseFlags(String... args) {
            final Map<String, String> flags = new HashMap<>();
            for (String arg : args) {
                final int separator = arg.indexOf('=');
                if (separator > 0 && separator < arg.length() - 1) {
                    flags.put(arg.substring(0, separator).trim().toLowerCase(Locale.ROOT), arg.substring(separator + 1).trim());
                }
            }
            return flags;
        }

        private int flagInt(Map<String, String> flags, String key, int fallback) {
            final String value = flags.get(key.toLowerCase(Locale.ROOT));
            if (value == null) {
                return fallback;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
    }

    @Singleton
    @SubCommand(TerrainCommand.class)
    private static class InfoSubCommand extends Command {

        @Override
        public String getName() {
            return "info";
        }

        @Override
        public String getDescription() {
            return "Report the current world's terrain scan";
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            final World world = player.getWorld();
            final File file = new File(world.getWorldFolder(), TerrainZones.MASK_FILE_NAME);
            if (!file.isFile()) {
                UtilMessage.message(player, "Clans", Component.text(
                        "'" + world.getName() + "' has not been scanned. Run /terrain scan.", NamedTextColor.YELLOW));
                return;
            }

            try {
                final TerrainMask mask = TerrainMask.load(file, world);
                UtilMessage.message(player, "Clans", Component.text("Terrain of '" + world.getName() + "': "
                        + mask.coveredChunks(TerrainType.OCEAN).size() + " ocean, "
                        + mask.coveredChunks(TerrainType.BEACH).size() + " beach, "
                        + mask.coveredChunks(TerrainType.MOUNTAIN).size() + " mountain chunks.", NamedTextColor.GREEN));
            } catch (IOException exception) {
                UtilMessage.message(player, "Clans",
                        Component.text("Failed to read terrain mask: " + exception.getMessage(), NamedTextColor.RED));
            }
        }
    }
}
