package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import me.mykindos.betterpvp.core.world.travel.ServerLocation;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Somewhere a player was: which site, which instance of it, and the exact spot.
 * <p>
 * Held as a value rather than a reference to a live {@link SiteInstance} because it has to outlive one. An instance
 * that was released while the player was away is the case {@link Residency} exists to recognise, and it can only
 * recognise it if the record still names the instance that is gone.
 */
@Value
public class Residence {

    @NotNull SiteKey site;
    @NotNull UUID instanceId;
    @NotNull ServerLocation location;

    public static @NotNull Residence of(@NotNull SiteInstance instance, @NotNull Location location) {
        return new Residence(instance.getKey(), instance.getId(), ServerLocation.local(location));
    }
}
