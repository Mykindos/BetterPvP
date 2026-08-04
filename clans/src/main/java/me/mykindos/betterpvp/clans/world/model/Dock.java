package me.mykindos.betterpvp.clans.world.model;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PointRegion;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.navigation.NavigatorNPC;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipInteractions;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Dock implements WorldContent {

    private final ClientManager clientManager;
    private final SceneObjectFactory objectFactory;

    public Dock(ClientManager clientManager, SceneObjectFactory objectFactory) {
        this.clientManager = clientManager;
        this.objectFactory = objectFactory;
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final List<Zone> zones = new ArrayList<>();

        // Rules
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        // Dock
        final List<CuboidRegion> dockOpt = regions.find("dock", CuboidRegion.class);
        for (CuboidRegion dock : dockOpt) {
            zones.add(Zone.builder()
                    .key(ClanZones.regionKey("dock"))
                    .displayName(Component.text("Dock"))
                    .bounds(RegionBounds.of(dock))
                    .priority(ClanZones.SERVER_REGION_PRIORITY + 5)
                    .rules(rules)
                    .build());
        }

        return zones;
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        final List<SceneSpawn> sceneObjects = new ArrayList<>();

        final ShipService shipService = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ShipService.class);
        final ShipInteractions shipInteractions = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ShipInteractions.class);

        for (PointRegion point : regions.find(ShipService.NAVIGATOR_POINT, PointRegion.class)) {
            // Which mooring this navigator works. Resolved when clicked, not now: the ship is moored by a region
            // contributor during the same load pass, so it may not exist yet at the moment this list is built.
            final String berthId = RegionTags.of(point).getString("ship", "").trim();
            final NavigatorNPC npc = new NavigatorNPC(this.objectFactory,
                    player -> board(player, world, berthId, shipService, shipInteractions));
            sceneObjects.add(new SceneSpawn(npc, point.getLocation(), this.objectFactory::backingEntity));
        }

        return sceneObjects;
    }

    private static void board(@NotNull Player player, @NotNull World world, @NotNull String berthId,
                              @NotNull ShipService shipService, @NotNull ShipInteractions shipInteractions) {
        if (berthId.isEmpty()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.navigator-unbound");
            return;
        }

        final Optional<Berth> berth = shipService.berth(world, berthId);
        if (berth.isEmpty()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.none-moored");
            return;
        }

        shipInteractions.board(player, berth.get());
    }
}
