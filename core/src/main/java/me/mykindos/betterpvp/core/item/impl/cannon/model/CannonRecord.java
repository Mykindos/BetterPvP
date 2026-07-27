package me.mykindos.betterpvp.core.item.impl.cannon.model;

import lombok.Value;
import lombok.With;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Everything needed to rebuild a player-placed cannon from nothing.
 * <p>
 * The record is the cannon's identity: it outlives the entity that represents it, and a cannon in an unloaded chunk
 * exists as a record with no body at all.
 */
@Value
public class CannonRecord {

    @NotNull UUID id;
    @NotNull String world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;
    @NotNull String archetypeId;
    @NotNull String propertiesJson;
    @Nullable UUID placedBy;

    /** Ammo id chambered when the record was last written, or {@code null} for an empty cannon. */
    @With @Nullable String ammoId;

    @With double health;

    /** Durable module metadata attached to the cannon. */
    @NotNull Map<String, String> tags;

    public static @NotNull CannonRecord of(@NotNull UUID id, @NotNull Location location, @NotNull String archetypeId,
                                           @NotNull CannonProperties properties, @Nullable UUID placedBy,
                                           @Nullable String ammoId, double health, @NotNull Map<String, String> tags) {
        return new CannonRecord(id, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), archetypeId, properties.toJson(), placedBy, ammoId, health,
                Map.copyOf(tags));
    }

    /** @return the anchor location, or {@code null} if the record's world is not currently loaded */
    public @Nullable Location toLocation() {
        final World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
