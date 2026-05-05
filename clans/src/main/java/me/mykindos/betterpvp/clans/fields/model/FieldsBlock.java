package me.mykindos.betterpvp.clans.fields.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import javax.annotation.Nullable;

/**
 * Represents an ore in the Fields zone.
 */
@Data
@EqualsAndHashCode(of = {"world", "x", "y", "z"})
public class FieldsBlock {

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final @Nullable BlockData data; // block data
    private long lastUsed; // when it was last mined, used for regeneration
    private boolean active; // if it is active or not

    // Set by Fields#addTemporaryBlock when an ore is materialized by a script
    // (e.g. an ability). Temporaries do not persist to the DB, do not
    // respawn after being mined, and revert to {@code previousData} when their
    // expiry passes without being mined.
    private boolean temporary;
    private long expiresAtMs;
    private @Nullable BlockData previousData;

    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public Location getLocation() {
        return new Location(getWorld(), x, y, z);
    }

    public Block getBlock() {
        return getLocation().getBlock();
    }

    public BlockData getBlockData() {
        return data;
    }

}
