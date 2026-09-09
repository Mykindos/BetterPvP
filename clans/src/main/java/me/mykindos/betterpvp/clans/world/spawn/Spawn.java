package me.mykindos.betterpvp.clans.world.spawn;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSites;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.clans.world.model.HumanCannon;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * What stands on Spawn, and the safety it is under. Written against the site rather than a world name, so every copy
 * of Spawn gets the same treatment once there is more than one.
 */
@Singleton
@PluginAdapter("Mapper")
public class Spawn implements WorldContent {

    private static final String SITE = "spawn";

    private final ClientManager clientManager;
    private final ClansSceneObjectFactory clansSceneFactory;
    private final HumanCannon humanCannon;

    @Inject
    private Spawn(@NotNull WorldContentService contentService, @NotNull WorldSites sites,
                  @NotNull ClientManager clientManager, @NotNull ClansSceneObjectFactory clansSceneFactory,
                  @NotNull CannonService cannonService) {
        this.clientManager = clientManager;
        this.clansSceneFactory = clansSceneFactory;
        this.humanCannon = new HumanCannon(cannonService);
        contentService.register(new WorldContentBinding(sites.selector(SITE), this::content));
    }

    private @NotNull List<WorldContent> content() {
        // Residents and props are not listed here: they belong to every world and register themselves, so spawn gets
        // them for the same reason any other place does - somebody drew the markers.
        final Dock dock = new Dock(clientManager, clansSceneFactory);
        return List.of(this, dock, this.humanCannon);
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final List<Zone> zones = new ArrayList<>();

        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        zones.add(Zone.builder()
                .key(ClanZones.regionKey("spawn"))
                .displayName(Component.text("Spawn"))
                .bounds(GlobalBounds.world(world))
                .tag(Zones.SAFE)
                .priority(ClanZones.SERVER_REGION_PRIORITY)
                .rules(rules)
                .build());

        return zones;
    }
}
