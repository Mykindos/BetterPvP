package me.mykindos.betterpvp.core.item.impl.cannon.model;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Somewhere a passenger cannon can fire a rider to.
 * <p>
 * Destinations are declared by whatever owns the cannon - an island's {@code WorldContent} reading them from Mapper
 * data-points, typically - and attached to the cannon, so core never has to know the names of any particular map's
 * landmarks.
 */
@Value
public class CannonDestination {

    @NotNull String name;
    @NotNull Location location;
    @NotNull Material icon;

    public static @NotNull CannonDestination of(@NotNull String name, @NotNull Location location) {
        return new CannonDestination(name, location, Material.GRASS_BLOCK);
    }
}
