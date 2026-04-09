package me.mykindos.betterpvp.hub.feature.ffa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PolygonRegion;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BlockVector;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class FFARegionService {

    private final PolygonRegion region;
    private final Location spawnpoint;
    private final Set<BlockVector> wallBlocks;
    private final BlockData wallData;

    @Inject
    public FFARegionService(HubWorld hubWorld) {
        this.region = hubWorld.findRegion("FFA", PolygonRegion.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hub world must define an FFA PolygonRegion"));
        this.spawnpoint = hubWorld.findRegion("ffa_spawnpoint", PerspectiveRegion.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hub world must define an ffa_spawnpoint PerspectiveRegion"))
                .getLocation()
                .clone();
        this.wallBlocks = computeWallBlocks();
        this.wallData = Material.RED_STAINED_GLASS.createBlockData();
    }

    public boolean contains(Location location) {
        return location != null
                && location.getWorld() == region.getWorld()
                && region.contains(location);
    }

    public Location getSpawnpoint() {
        return spawnpoint.clone();
    }

    public Set<BlockVector> getNearbyWallBlocks(Location center, double radius) {
        final double radiusSquared = radius * radius;
        return wallBlocks.stream()
                .filter(vector -> {
                    final double dx = vector.getBlockX() + 0.5 - center.getX();
                    final double dy = vector.getBlockY() + 0.5 - center.getY();
                    final double dz = vector.getBlockZ() + 0.5 - center.getZ();
                    return dx * dx + dy * dy + dz * dz <= radiusSquared;
                })
                .collect(Collectors.toSet());
    }

    public Location toLocation(BlockVector vector) {
        return new Location(region.getWorld(), vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public BlockData getWallData() {
        return wallData;
    }

    private Set<BlockVector> computeWallBlocks() {
        final Set<BlockVector> result = new HashSet<>();
        for (CuboidRegion child : region.getChildren()) {
            final int minX = child.getMin().getBlockX();
            final int minY = child.getMin().getBlockY();
            final int minZ = child.getMin().getBlockZ();
            final int maxX = child.getMax().getBlockX();
            final int maxY = child.getMax().getBlockY();
            final int maxZ = child.getMax().getBlockZ();

            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    addWallBlock(result, minX - 1, y, z);
                    addWallBlock(result, maxX + 1, y, z);
                }
                for (int x = minX; x <= maxX; x++) {
                    addWallBlock(result, x, y, minZ - 1);
                    addWallBlock(result, x, y, maxZ + 1);
                }
            }
        }
        return result;
    }

    private void addWallBlock(Set<BlockVector> result, int x, int y, int z) {
        if (contains(new Location(region.getWorld(), x, y, z))) {
            return;
        }

        final World world = region.getWorld();
        final Block block = world.getBlockAt(x, y, z);
        if (!block.isPassable() && !block.getType().isAir()) {
            return;
        }

        final boolean touchesArena = contains(new Location(world, x + 1, y, z))
                || contains(new Location(world, x - 1, y, z))
                || contains(new Location(world, x, y, z + 1))
                || contains(new Location(world, x, y, z - 1));
        if (touchesArena) {
            result.add(new BlockVector(x, y, z));
        }
    }
}
