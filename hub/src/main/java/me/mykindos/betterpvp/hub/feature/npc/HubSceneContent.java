package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.npc.KitSelector;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.SelectableServerType;
import me.mykindos.betterpvp.core.framework.ServerTypes;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.scene.display.SceneTextDisplay;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.hub.model.HubWorld;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Spawns the hub NPCs, text displays and kit selectors from Mapper data-points once ModelEngine has registered its
 * models.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class HubSceneContent implements WorldContent {

    private final Hub hub;
    private final HubNPCFactory npcFactory;
    private final CooldownManager cooldownManager;
    private final NetworkPlayerCountService networkPlayerCountService;
    private final HubQueueStatusRegistry queueStatusRegistry;
    private final OrchestrationGateway orchestrationGateway;

    @Inject
    public HubSceneContent(Hub hub, HubNPCFactory npcFactory, CooldownManager cooldownManager,
                           NetworkPlayerCountService networkPlayerCountService, HubWorld hubWorld,
                           HubQueueStatusRegistry queueStatusRegistry, OrchestrationGateway orchestrationGateway,
                           WorldContentService contentService) {
        this.hub = hub;
        this.npcFactory = npcFactory;
        this.cooldownManager = cooldownManager;
        this.networkPlayerCountService = networkPlayerCountService;
        this.queueStatusRegistry = queueStatusRegistry;
        this.orchestrationGateway = orchestrationGateway;
        contentService.register(hub, new WorldContentBinding(WorldSelector.named(hubWorld.getName()),
                () -> List.of(this)).withRequiresModels(true));
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        final Set<String> keys = ModelEngineAPI.getAPI().getModelRegistry().getKeys();
        if (!keys.contains("dummy") || !keys.contains("chest_shadow")) {
            log.warn("ModelEngine models 'dummy' or 'chest_shadow' are not registered - skipping Hub NPC load").submit();
            return;
        }

        scope.adopt(npcFactory.spawn(new StoreChestNPC(npcFactory, cooldownManager, hub),
                generateDummyEntity(point(regions, "npc_store"))));

        final Location ffaSpawnpoint = point(regions, "ffa_spawnpoint");
        scope.adopt(npcFactory.spawn(new TrainerNPC(npcFactory, cooldownManager, ffaSpawnpoint),
                generateDummyEntity(point(regions, "npc_trainer"))));

        spawnDisplay(scope, point(regions, "ffa_arena_display"),
                Component.text("FFA Arena", TextColor.color(0xffbb00), TextDecoration.BOLD), 2.5f);
        spawnDisplay(scope, point(regions, "ffa_kits_equipped_display"),
                Component.text("Kits are equipped upon entering", TextColor.color(0xFF0000), TextDecoration.BOLD), 1.3f);

        for (Role role : Role.values()) {
            final KitSelector selector = new KitSelector(role, false, true);
            selector.spawn(point(regions, "kit_selector_" + role.name().toLowerCase()));
            scope.onRelease(selector::remove);
        }

        scope.adopt(spawnInstanceSelector(point(regions, "npc_selector_classic"), ServerTypes.CLANS_CLASSIC, true));
        scope.adopt(spawnInstanceSelector(point(regions, "npc_selector_champions"), ServerTypes.CHAMPIONS, false));

        for (PerspectiveRegion region : regions.find("npc_coming_soon", PerspectiveRegion.class)) {
            scope.adopt(npcFactory.spawn(new ComingSoonNPC(npcFactory), generateDummyEntity(region.getLocation())));
        }
    }

    private @NotNull Location point(@NotNull RegionIndex regions, @NotNull String name) {
        return regions.findOne(name, PerspectiveRegion.class)
                .orElseThrow(() -> new IllegalStateException("Hub world has no data-point '" + name
                        + "' of type PerspectiveRegion"))
                .getLocation();
    }

    private NPC spawnInstanceSelector(@NotNull Location location, @NotNull SelectableServerType serverType, boolean featured) {
        final IronGolem golem = generateDummyEntity(location);
        final Component title = serverType.getDisplayTitle();

        if (featured) {
            final TextColor[] gradient = new TextColor[]{
                    TextColor.color(255, 226, 36),
                    TextColor.color(255, 182, 23),
                    TextColor.color(255, 116, 23),
            };
            return npcFactory.spawn(
                    new InstanceSelectorNPC.Featured(npcFactory, title, gradient, serverType,
                            networkPlayerCountService, queueStatusRegistry, orchestrationGateway),
                    golem);
        } else {
            return npcFactory.spawn(
                    new InstanceSelectorNPC(npcFactory, title, serverType,
                            networkPlayerCountService, queueStatusRegistry, orchestrationGateway),
                    golem);
        }
    }

    private void spawnDisplay(@NotNull WorldContentScope scope, @NotNull Location location, @NotNull Component text,
                              float scale) {
        scope.spawn(new SceneTextDisplay(text, scale, Display.Billboard.FIXED),
                location.getWorld().spawn(location, TextDisplay.class));
    }

    @NotNull
    private static IronGolem generateDummyEntity(@NotNull Location location) {
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class);
        golem.setAI(false);
        golem.setInvulnerable(true);
        golem.setCollidable(false);
        golem.setPersistent(false);
        golem.setInvisible(true);
        return golem;
    }
}
