package me.mykindos.betterpvp.clans.world.model;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.navigation.NavigatorNPC;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Dock implements WorldContent {

    private final ClientManager clientManager;
    private final SceneObjectFactory objectFactory;
    private final List<Island> destinations;

    public Dock(ClientManager clientManager, SceneObjectFactory objectFactory, List<Island> destinations) {
        this.clientManager = clientManager;
        this.objectFactory = objectFactory;
        this.destinations = destinations;
    }

    public Collection<Island> getDestinations() {
        return Collections.unmodifiableList(destinations);
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull Collection<Region> regions) {
        final List<Zone> zones = new ArrayList<>();

        // Rules
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        // Dock
        final List<CuboidRegion> dockOpt = MapperHelper.findRegions(regions, "dock", CuboidRegion.class);
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
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull Collection<Region> regions) {
        final List<SceneSpawn> sceneObjects = new ArrayList<>();

        // Navigator NPC
        final List<PointRegion> npcNavigator = MapperHelper.findRegions(regions, "npc_navigator", PointRegion.class);
        for (PointRegion point : npcNavigator) {
            final Location location = point.getLocation();
            final NavigatorNPC npc = new NavigatorNPC(this.objectFactory, this.destinations);
            final SceneSpawn spawn = new SceneSpawn(npc, location, this.objectFactory::backingEntity);
            sceneObjects.add(spawn);
        }

        return sceneObjects;
    }
}
