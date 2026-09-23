package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Named places inside a site that a traveller can be sent to instead of its arrival point, such as a camp's Barracks
 * for somebody respawning. The module that owns the site registers them, and only the server holding the instance
 * resolves one, so the name is all that travels.
 */
@Singleton
public class SiteLandings {

    private final Map<String, Function<World, Optional<Location>>> landings = new ConcurrentHashMap<>();

    public void register(@NotNull String siteId, @NotNull String landing,
                         @NotNull Function<World, Optional<Location>> resolver) {
        landings.put(key(siteId, landing), resolver);
    }

    /** Where {@code landing} is in {@code world}, or empty to fall back to the site's arrival point. */
    public @NotNull Optional<Location> resolve(@NotNull String siteId, @NotNull String landing, @NotNull World world) {
        final Function<World, Optional<Location>> resolver = landings.get(key(siteId, landing));
        return resolver == null ? Optional.empty() : resolver.apply(world);
    }

    private static @NotNull String key(@NotNull String siteId, @NotNull String landing) {
        return siteId + ":" + landing;
    }
}
