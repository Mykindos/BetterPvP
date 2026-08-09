package me.mykindos.betterpvp.clans.world.terrain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.terrain.TerrainMask;
import me.mykindos.betterpvp.core.world.terrain.TerrainType;
import me.mykindos.betterpvp.core.world.zone.ColumnMaskBounds;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns any world's saved {@link TerrainMask} into capability zones — an ocean, a beach, and a mountain — each covering
 * the arbitrarily-shaped footprint a scan produced. The interior is left unzoned, which is what keeps it claimable and
 * buildable while the ring around it is protected.
 * <p>
 * Registered on {@link WorldSelector#any() every world}, so a world simply needs a {@code terrain-mask.dat} to get its
 * terrain zones — no per-world wiring. A world with no mask (never scanned, or still being built) contributes nothing.
 * Because it rides {@link WorldContent}, the {@link WorldContentService} owns its lifecycle: rescanning and reloading
 * tear the old zones down and rebuild from the new mask.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class TerrainZoneContent implements WorldContent {

    private final ClientManager clientManager;

    @Inject
    public TerrainZoneContent(@NotNull ClientManager clientManager, @NotNull WorldContentService contentService) {
        this.clientManager = clientManager;
        contentService.register(new WorldContentBinding(WorldSelector.any(), () -> List.of(this)));
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final File file = new File(world.getWorldFolder(), TerrainZones.MASK_FILE_NAME);
        if (!file.isFile()) {
            return List.of();
        }

        final TerrainMask mask;
        try {
            mask = TerrainMask.load(file, world);
        } catch (IOException exception) {
            log.error("Failed to load terrain mask for '{}'", world.getName(), exception).submit();
            return List.of();
        }
        if (mask.isEmpty()) {
            return List.of();
        }

        final ZoneRuleContainer rules = new ZoneRuleContainer().add(new NoBuildRule(clientManager));
        final List<Zone> zones = new ArrayList<>();
        addZone(zones, world, mask, TerrainType.OCEAN, "Ocean", rules);
        addZone(zones, world, mask, TerrainType.BEACH, "Beach", rules);
        addZone(zones, world, mask, TerrainType.MOUNTAIN, "Mountain", rules);
        return zones;
    }

    private void addZone(@NotNull List<Zone> zones, @NotNull World world, @NotNull TerrainMask mask,
                         @NotNull TerrainType type, @NotNull String display, @NotNull ZoneRuleContainer rules) {
        final ColumnMaskBounds bounds = new ColumnMaskBounds(mask, type);
        if (bounds.coveredChunks().isEmpty()) {
            return; // nothing of this type was classified; don't register an empty ambient zone
        }

        // Each zone carries its type tag ("ocean"/"beach"/"mountain") so it matches clans.map.zones.<tag> and, for
        // ocean, drives the ocean-damage tick; plus no_build for territory protection.
        zones.add(Zone.builder()
                .key(TerrainZones.key(world, type))
                .displayName(Component.text(display))
                .bounds(bounds)
                .priority(ClanZones.SERVER_REGION_PRIORITY)
                .tag(Zones.NO_BUILD)
                .tag(type.name().toLowerCase(Locale.ROOT))
                .rules(rules)
                .build());
    }
}
