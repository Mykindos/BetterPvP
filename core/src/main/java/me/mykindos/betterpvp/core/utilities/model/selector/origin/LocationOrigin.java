package me.mykindos.betterpvp.core.utilities.model.selector.origin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * A selector origin based on a fixed location.
 * Orientation is derived from the location's direction if it has one.
 *
 * @param location the location to use as the origin
 */
public record LocationOrigin(Location location) implements SelectorOrigin {

    @Override
    public World getWorld() {
        return location.getWorld();
    }

    @Override
    public Vector getPosition() {
        return location.toVector();
    }

    @Override
    public Location toLocation() {
        return location.clone();
    }

    @Override
    public @Nullable Vector getOrientation() {
        return location.getDirection();
    }

    @Override
    public float getPitch() {
        return location.getPitch();
    }

    @Override
    public float getYaw() {
        return location.getYaw();
    }
}
