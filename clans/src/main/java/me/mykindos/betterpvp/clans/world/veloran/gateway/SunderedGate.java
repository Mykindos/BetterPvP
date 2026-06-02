package me.mykindos.betterpvp.clans.world.veloran.gateway;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.veloran.Veloran;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <b>The Sundered Gate</b> — Veloran's one and only entrance: a vast, dark, near-consumed portal torn open at Fields.
 * <p>
 * A self-contained {@link WorldContent}: from the single {@link #REGION_NAME} Mapper cuboid it contributes its
 * capability {@link #zones zone} (carved out of the surrounding Fields region) and its portal {@link #sceneObjects
 * prop}, both resolved from the same volume so the protected area and the teleport trigger are guaranteed identical.
 * It owns its own {@link GatewayPropFactory}, so the loaders that install it never see a factory.
 */
@CustomLog
@Singleton
public class SunderedGate implements WorldContent {

    /** Mapper cuboid data-point that defines the generic area */
    public static final String REGION_NAME = "clans:gateway_sundered";

    /** Portal cuboid region that defines the portal volume (the gate's bounds and teleport trigger). */
    public static final String PORTAL_NAME = "clans:gateway_sundered_portal";

    /** Portal perspective region that defines the portal text display entity. */
    public static final String PORTAL_MARKER_NAME = "clans:gateway_sundered_portal_marker";

    /** Tag marking the zone (and prop) as the gateway. */
    public static final String TAG = "gateway";

    /** Stable zone identity, keyed consistently with the other clans server-region zones. */
    public static final Key ZONE_KEY = ClanZones.regionKey("The Sundered Gate");

    /** Ominous label rendered floating above the portal. */
    public static final Component LABEL = Component.text("The Sundered Gate", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD);

    private final Provider<Veloran> veloranProvider;
    private final GatewayPropFactory propFactory;
    private final ClientManager clientManager;

    @Inject
    public SunderedGate(Provider<Veloran> veloranProvider, GatewayPropFactory propFactory, ClientManager clientManager) {
        this.veloranProvider = veloranProvider;
        this.propFactory = propFactory;
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull Collection<Region> regions) {
        return MapperHelper.findRegion(regions, REGION_NAME, CuboidRegion.class)
                .map(cuboid -> {
                    cuboid.setWorld(world);
                    return cuboid;
                })
                .map(cuboid -> List.of(buildZone(cuboid)))
                .orElseGet(() -> {
                    log.warn("The Sundered Gate has no '{}' Mapper cuboid - zone not loaded", REGION_NAME).submit();
                    return List.of();
                });
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull Collection<Region> regions) {
        final Optional<CuboidRegion> portalRegion = MapperHelper.findRegion(regions, PORTAL_NAME, CuboidRegion.class);
        if (portalRegion.isEmpty()) {
            log.warn("The Sundered Gate has no '{}' Mapper perspective region - portal not spawned", PORTAL_NAME).submit();
            return List.of();
        }

        final Optional<PerspectiveRegion> markerRegion = MapperHelper.findRegion(regions, PORTAL_MARKER_NAME, PerspectiveRegion.class);
        if (markerRegion.isEmpty()) {
            log.warn("The Sundered Gate has no '{}' Mapper perspective region - portal label not spawned", PORTAL_MARKER_NAME).submit();
            return List.of();
        }

        portalRegion.get().setWorld(world);
        markerRegion.get().setWorld(world);

        final Location markerLocation = markerRegion.get().getLocation();
        final GatewayProp prop = new GatewayProp(propFactory,
                clientManager,
                veloranProvider,
                markerLocation,
                portalRegion.get(),
                LABEL);
        return List.of(new SceneSpawn(prop, spawnLabelEntity(world, markerLocation)));
    }

    /**
     * Builds the gate's capability zone: a sub-zone that out-ranks the surrounding Fields region on overlap and is
     * {@link Zones#NO_BUILD protected} so the portal structure can't be griefed.
     */
    private Zone buildZone(@NotNull CuboidRegion cuboid) {
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        return Zone.builder()
                .key(ZONE_KEY)
                .displayName(LABEL)
                .bounds(RegionBounds.of(cuboid))
                // Sits inside Fields - must out-rank it (and the other server regions) on overlap.
                .priority(ClanZones.SERVER_REGION_PRIORITY + 5)
                .tag(TAG)
                .tag(Zones.NO_BUILD)
                .rules(rules)
                .build();
    }

    /** Spawns the backing label entity at the top-centre of the portal volume. */
    private TextDisplay spawnLabelEntity(@NotNull World world, @NotNull Location labelLocation) {
        return world.spawn(labelLocation, TextDisplay.class);
    }
}
