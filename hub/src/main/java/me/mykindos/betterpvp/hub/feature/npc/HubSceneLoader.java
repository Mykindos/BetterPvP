package me.mykindos.betterpvp.hub.feature.npc;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.npc.KitSelector;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.SelectableServerType;
import me.mykindos.betterpvp.core.framework.ServerTypes;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.scene.display.SceneTextDisplay;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.MapperSceneLoader;
import me.mykindos.betterpvp.core.scene.loader.ModelEngineLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.npc.NPC;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Loads all hub NPCs, text displays, and kit selectors from Mapper data-points whenever
 * ModelEngine finalizes its pipeline. Replaces the old {@code HubNPCListener} + the
 * {@code HubNPCFactory#loadConfig()} / {@code #tryLoad()} lifecycle.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class HubSceneLoader extends MapperSceneLoader {

    private final HubNPCFactory npcFactory;
    private final CooldownManager cooldownManager;
    private final NetworkPlayerCountService networkPlayerCountService;
    private final Hub hub;
    private final HubWorld hubWorld;
    private final HubQueueStatusRegistry queueStatusRegistry;
    private final OrchestrationGateway orchestrationGateway;

    /** Kit selectors — not SceneObjects, cleaned up in {@link #unload()}. */
    private final List<KitSelector> kitSelectors = new ArrayList<>();

    @Inject
    public HubSceneLoader(Hub hub, HubNPCFactory npcFactory, CooldownManager cooldownManager,
                          NetworkPlayerCountService networkPlayerCountService, HubWorld hubWorld,
                          HubQueueStatusRegistry queueStatusRegistry, OrchestrationGateway orchestrationGateway,
                          SceneLoaderManager sceneLoaderManager) {
        this.hub = hub;
        this.npcFactory = npcFactory;
        this.cooldownManager = cooldownManager;
        this.networkPlayerCountService = networkPlayerCountService;
        this.hubWorld = hubWorld;
        this.queueStatusRegistry = queueStatusRegistry;
        this.orchestrationGateway = orchestrationGateway;
        sceneLoaderManager.register(this, hub);
    }

    @Override
    @NotNull
    protected Collection<Region> getRegions() {
        return hubWorld.getRegions();
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ModelEngineLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void unload() {
        // SceneTextDisplay objects are in managed and removed automatically.
        // KitSelectors are not SceneObjects, so remove them manually here.
        for (KitSelector selector : kitSelectors) {
            selector.remove();
        }
        kitSelectors.clear();
    }

    @Override
    protected void load() {
        final Set<String> keys = ModelEngineAPI.getAPI().getModelRegistry().getKeys();
        if (!keys.contains("dummy") || !keys.contains("chest_shadow")) {
            log.warn("ModelEngine models 'dummy' or 'chest_shadow' are not registered — skipping Hub NPC load").submit();
            return;
        }

        final World world = hubWorld.getWorld();
        Preconditions.checkNotNull(world, "Hub world is not loaded");

        // Store chest NPC
        final Location storeLocation = getDataPoint("npc_store", PerspectiveRegion.class).getLocation();
        storeLocation.setWorld(world);
        track(npcFactory.spawnNPC(new StoreChestNPC(npcFactory, cooldownManager, hub), generateDummyEntity(storeLocation)));

        // Trainer / FFA-arena NPC
        final Location trainerLocation = getDataPoint("npc_trainer", PerspectiveRegion.class).getLocation();
        trainerLocation.setWorld(world);
        final Location ffaSpawnpoint = getDataPoint("ffa_spawnpoint", PerspectiveRegion.class).getLocation();
        ffaSpawnpoint.setWorld(world);
        track(npcFactory.spawnNPC(new TrainerNPC(npcFactory, cooldownManager, ffaSpawnpoint), generateDummyEntity(trainerLocation)));

        // Floating text displays — tracked as SceneObjects so they auto-remove on reload
        final Location ffaArenaDisplayLocation = getDataPoint("ffa_arena_display", PerspectiveRegion.class).getLocation();
        ffaArenaDisplayLocation.setWorld(world);
        spawnDisplay(ffaArenaDisplayLocation,
                Component.text("FFA Arena", TextColor.color(0xffbb00), TextDecoration.BOLD), 2.5f);

        final Location ffaKitsEquippedDisplayLocation = getDataPoint("ffa_kits_equipped_display", PerspectiveRegion.class).getLocation();
        ffaKitsEquippedDisplayLocation.setWorld(world);
        spawnDisplay(ffaKitsEquippedDisplayLocation,
                Component.text("Kits are equipped upon entering", TextColor.color(0xFF0000), TextDecoration.BOLD), 1.3f);

        // Kit selectors (one per champion role)
        spawnKitSelectors(world);

        // Instance-selector NPC (Classic)
        final Location classicLocation = getDataPoint("npc_selector_classic", PerspectiveRegion.class).getLocation();
        classicLocation.setWorld(world);
        track(spawnInstanceSelector(classicLocation, ServerTypes.CLANS_CLASSIC, false));

        // Coming-soon NPCs (multiple data-points with the same name)
        for (PerspectiveRegion region : getDataPoints("npc_coming_soon", PerspectiveRegion.class)) {
            final Location comingSoonLocation = region.getLocation();
            comingSoonLocation.setWorld(world);
            track(npcFactory.spawnNPC(new ComingSoonNPC(npcFactory), generateDummyEntity(comingSoonLocation)));
        }

        log.info("Hub scene loaded - {} kit-selector(s)", kitSelectors.size()).submit();
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
            return npcFactory.spawnNPC(
                    new InstanceSelectorNPC.Featured(npcFactory, title, gradient, serverType,
                            networkPlayerCountService, queueStatusRegistry, orchestrationGateway),
                    golem);
        } else {
            return npcFactory.spawnNPC(
                    new InstanceSelectorNPC(npcFactory, title, serverType,
                            networkPlayerCountService, queueStatusRegistry, orchestrationGateway),
                    golem);
        }
    }

    private void spawnKitSelectors(@NotNull World world) {
        for (Role role : Role.values()) {
            final String roleName = role.name().toLowerCase();
            final Location location = getDataPoint("kit_selector_" + roleName, PerspectiveRegion.class).getLocation();
            location.setWorld(world);
            final KitSelector selector = new KitSelector(role, false, true);
            selector.spawn(location);
            kitSelectors.add(selector);
        }
    }

    /**
     * Spawns a standalone {@link SceneTextDisplay} at the given location and tracks it
     * in the loader's managed set so it is automatically removed on reload.
     */
    private void spawnDisplay(@NotNull Location location, @NotNull Component text, float scale) {
        spawn(new SceneTextDisplay(text, scale, Display.Billboard.FIXED),
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
