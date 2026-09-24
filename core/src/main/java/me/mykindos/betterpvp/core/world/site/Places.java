package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Describes a location on this server as {@link Whereabouts}. */
@Singleton
public class Places {

    private final SiteInstances instances;
    private final SiteRegistry registry;
    private final ZoneManager zones;

    @Inject
    public Places(@NotNull SiteInstances instances, @NotNull SiteRegistry registry, @NotNull ZoneManager zones) {
        this.instances = instances;
        this.registry = registry;
        this.zones = zones;
    }

    public @NotNull Whereabouts of(@NotNull Location location) {
        final World world = Objects.requireNonNull(location.getWorld(), "location has no world");
        final Component place = instances.byWorld(world.getName())
                .flatMap(instance -> registry.get(instance.getKey().getSiteId()))
                .map(Site::getDisplayName)
                .orElseGet(() -> Component.text(world.getName()));
        final Zone zone = zones.getZoneAt(location);
        return new Whereabouts(Core.getCurrentRealm().getServer().getName(), place,
                zone == null ? null : zone.getDisplayName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
