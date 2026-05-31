package me.mykindos.betterpvp.hub.feature.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import me.mykindos.betterpvp.hub.feature.ffa.FFARegionService;
import me.mykindos.betterpvp.hub.model.HubWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.event.Listener;

/**
 * Registers the hub's zones with the core {@link ZoneManager} at startup. Instantiated eagerly as a
 * {@link BPvPListener} (it carries no handlers; the work happens in the constructor, once its dependencies — and so the
 * hub world and FFA region — are built).
 * <p>
 * COMMON is the hub world's default zone (returned when no other zone matches). FFA wraps the FFA polygon region and
 * sits at a higher priority so it wins inside the arena.
 */
@BPvPListener
@Singleton
public class HubZoneRegistrar implements Listener {

    @Inject
    private HubZoneRegistrar(ZoneManager zoneManager, HubWorld hubWorld, FFARegionService ffaRegionService) {
        final World world = hubWorld.getWorld();

        final Zone common = Zone.builder()
                .key(HubZones.COMMON)
                .displayName(Component.text("Common"))
                .bounds(GlobalBounds.world(world))
                .priority(0)
                .tag(Zones.SAFE)
                .build();
        zoneManager.setDefaultZone(world, common);

        final Zone ffa = Zone.builder()
                .key(HubZones.FFA)
                .displayName(Component.text("FFA"))
                .bounds(RegionBounds.of(ffaRegionService.getRegion()))
                .priority(10)
                .tag(Zones.PVP)
                .build();
        zoneManager.register(ffa);
    }
}
