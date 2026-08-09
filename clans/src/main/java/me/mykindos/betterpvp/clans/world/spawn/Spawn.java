package me.mykindos.betterpvp.clans.world.spawn;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.aldenmark.Aldenmark;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
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

@Singleton
@PluginAdapter("Mapper")
public class Spawn extends Island implements WorldContent {

    private final ClientManager clientManager;
    private final ClansSceneObjectFactory clansSceneFactory;
    private final HumanCannon humanCannon;
    private final Provider<Aldenmark> aldenmark; // circular dependency

    @Inject
    private Spawn(@NotNull WorldContentService contentService, ClientManager clientManager,
                  ClansSceneObjectFactory clansSceneFactory, CannonService cannonService,
                  Provider<Aldenmark> aldenmark) {
        super(contentService);
        this.clientManager = clientManager;
        this.clansSceneFactory = clansSceneFactory;
        this.humanCannon = new HumanCannon(cannonService);
        this.aldenmark = aldenmark;
    }

    @Override
    public @NotNull List<WorldContent> content() {
        // Residents and props are not listed here: they belong to every world and register themselves, so spawn gets
        // them for the same reason any other island does - somebody drew the markers.
        final Dock dock = new Dock(clientManager, clansSceneFactory);
        return List.of(this, dock, this.humanCannon);
    }

    @Override
    public @NotNull String worldName() {
        return "Season-2/Spawn";
    }

    @Override
    public @NotNull String name() {
        return "Spawn";
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final List<Zone> zones = new ArrayList<>();

        // Rules
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        // Spawn
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
