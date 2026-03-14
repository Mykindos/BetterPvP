package me.mykindos.betterpvp.core.tracking.model;

import org.bukkit.Location;
import org.bukkit.Chunk;

/**
 * Identifies a 16x16 block grid cell aligned to Minecraft chunks.
 * Uses chunk coordinates (not block coordinates) as the key, keeping
 * it consistent with the existing clan territory chunk format.
 */
public record GridKey(String world, int chunkX, int chunkZ) {

    public static GridKey of(Location location) {
        return new GridKey(
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        );
    }

    public static GridKey of(Chunk chunk) {
        return new GridKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

}
