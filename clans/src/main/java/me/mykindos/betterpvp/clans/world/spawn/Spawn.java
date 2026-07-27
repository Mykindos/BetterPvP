package me.mykindos.betterpvp.clans.world.spawn;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.aldenmark.Aldenmark;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.clans.world.model.HumanCannon;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
@PluginAdapter("Mapper")
public class Spawn extends Island implements WorldContent {

    private final ClientManager clientManager;
    private final ClansSceneObjectFactory clansSceneFactory;
    private final HumanCannon humanCannon;
    private final Provider<Aldenmark> aldenmark; // circular dependency

    @Inject
    private Spawn(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry, @NotNull SceneLoaderManager loaderManager,
                  @NotNull Clans clans, ClientManager clientManager, ClansSceneObjectFactory clansSceneFactory,
                  CannonService cannonService, Provider<Aldenmark> aldenmark) {
        super(zoneManager, sceneRegistry, loaderManager, clans);
        this.clientManager = clientManager;
        this.clansSceneFactory = clansSceneFactory;
        this.humanCannon = new HumanCannon(cannonService);
        this.aldenmark = aldenmark;
    }

    @Override
    public @NotNull List<WorldContent> content() {
        final Dock dock = new Dock(clientManager, clansSceneFactory, List.of(aldenmark.get()));
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
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull Collection<Region> regions) {
        final List<Zone> zones = new ArrayList<>();

        // Rules
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        // Spawn
        zones.add(Zone.builder()
                .key(ClanZones.regionKey("spawn"))
                .displayName(Component.text("Spawn"))
                .bounds(GlobalBounds.world(world))
                .priority(ClanZones.SERVER_REGION_PRIORITY)
                .rules(rules)
                .build());

        return zones;
    }
}
