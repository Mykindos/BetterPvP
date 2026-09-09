package me.mykindos.betterpvp.core.world.travel;

import lombok.Value;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A location that may live on another server. Wraps a {@link Location} with the name of the server it belongs to, so
 * a stored origin remains meaningful even if the player who recorded it is later moved to a different backend server
 * entirely.
 */
@Value
public class ServerLocation {

    @NotNull String server;
    @NotNull Location location;

    /**
     * @return whether this location is on the server this code is currently running on
     */
    public boolean isLocal() {
        return server.equals(Core.getCurrentRealm().getServer().getName());
    }

    /**
     * @return the location, if it is both local and its world is currently loaded
     */
    public @NotNull Optional<Location> toLocation() {
        if (!isLocal() || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.of(location);
    }

    /**
     * @return this location tagged with the current server, ready to be stored
     */
    public static @NotNull ServerLocation local(@NotNull Location location) {
        return new ServerLocation(Core.getCurrentRealm().getServer().getName(), location);
    }

    public @NotNull String serialize() {
        return server + "|" + UtilWorld.locationToString(location, false);
    }

    /**
     * Parses a stored origin. Values written before server-qualification carry no {@code server|} prefix, and those
     * are read as belonging to the current server, which is what they originally meant.
     */
    public static @NotNull ServerLocation parse(@NotNull String stored) {
        final int separator = stored.indexOf('|');
        if (separator < 0) {
            return new ServerLocation(Core.getCurrentRealm().getServer().getName(), UtilWorld.stringToLocation(stored));
        }

        final String server = stored.substring(0, separator);
        final Location location = UtilWorld.stringToLocation(stored.substring(separator + 1));
        return new ServerLocation(server, location);
    }
}
