package me.mykindos.betterpvp.hub.feature.npc;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.champions.champions.npc.KitSelector;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.ClansServerType;
import me.mykindos.betterpvp.core.framework.ServerTypes;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.model.HubWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class HubNPCFactory extends NPCFactory implements ConfigAccessor {

    private final List<NPC> npcs = new ArrayList<>();
    private final CooldownManager cooldownManager;
    private final NetworkPlayerCountService networkPlayerCountService;
    private final Hub hub;
    private final HubWorld hubWorld;
    private final List<KitSelector> kitSelectors = new ArrayList<>();
    private final List<Entity> displays = new ArrayList<>();

    @Inject
    private HubNPCFactory(NPCRegistry registry, CooldownManager cooldownManager, NetworkPlayerCountService networkPlayerCountService,
                          Hub hub, HubWorld hubWorld) {
        super("hub", registry);
        this.cooldownManager = cooldownManager;
        this.networkPlayerCountService = networkPlayerCountService;
        this.hub = hub;
        this.hubWorld = hubWorld;
    }

    public boolean tryLoad(Hub hub) {
        final Set<String> keys = ModelEngineAPI.getAPI().getModelRegistry().getKeys();
        if (!keys.contains("dummy") || !keys.contains("chest_shadow")) {
            return false;
        }

        loadConfig(hub.getConfig());
        return true;
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        for (NPC npc : npcs) {
            npc.remove();
        }
        npcs.clear();
        for (KitSelector selector : kitSelectors) {
            selector.remove();
        }
        kitSelectors.clear();
        for (Entity display : displays) {
            display.remove();
        }
        displays.clear();

        final World world = hubWorld.getWorld();
        Preconditions.checkNotNull(world, "Hub world is not loaded");

        final Location storeLocation = getDataPoint("npc_store", PerspectiveRegion.class).getLocation();
        storeLocation.setWorld(world);
        this.npcs.add(this.spawnStore(storeLocation));

        final Location trainerLocation = getDataPoint("npc_trainer", PerspectiveRegion.class).getLocation();
        trainerLocation.setWorld(world);
        final Location ffaSpawnpoint = getDataPoint("ffa_spawnpoint", PerspectiveRegion.class).getLocation();
        ffaSpawnpoint.setWorld(world);
        this.npcs.add(this.spawnTrainer(trainerLocation, ffaSpawnpoint));

        final Location ffaArenaDisplayLocation = getDataPoint("ffa_arena_display", PerspectiveRegion.class).getLocation();
        ffaArenaDisplayLocation.setWorld(world);
        this.displays.add(this.spawnFfaArenaDisplay(ffaArenaDisplayLocation));

        this.spawnKitSelectors(world);

        final Location classicLocation = getDataPoint("npc_selector_classic", PerspectiveRegion.class).getLocation();
        classicLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(classicLocation, ServerTypes.CLANS_CLASSIC, false));

        for (PerspectiveRegion region : getDataPoints("npc_coming_soon", PerspectiveRegion.class)) {
            final Location comingSoonLocation = region.getLocation();
            comingSoonLocation.setWorld(world);
            this.npcs.add(this.spawnComingSoon(comingSoonLocation));
        }

//        final Location squadsLocation = getDataPoint("npc_selector_squads", PerspectiveRegion.class).getLocation();
//        squadsLocation.setWorld(world);
//        this.npcs.add(this.spawnInstanceSelector(squadsLocation, ServerTypes.CLANS_SQUADS, false));

//        final Location casualLocation = getDataPoint("npc_selector_casual", PerspectiveRegion.class).getLocation();
//        casualLocation.setWorld(world);
//        this.npcs.add(this.spawnInstanceSelector(casualLocation, ServerTypes.CLANS_CASUAL, true));
    }

    private <T extends Region> T getDataPoint(String name, Class<T> type) {
        return this.hubWorld.getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No region found with name: " + name));
    }

    private <T extends Region> List<T> getDataPoints(String name, Class<T> type) {
        return this.hubWorld.getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private NPC spawnInstanceSelector(@NotNull Location location, ClansServerType serverType, boolean featured) {
        final IronGolem golem = generateDummyEntity(location);
        final Component title = serverType.getDisplayTitle();

        final NPC npc;
        if (featured) {
            final TextColor[] gradient = new TextColor[]{
                    TextColor.color(255, 226, 36),
                    TextColor.color(255, 182, 23),
                    TextColor.color(255, 116, 23),
            };
            npc = new InstanceSelectorNPC.Featured(this, golem, title, gradient, serverType, networkPlayerCountService);
        } else {
            npc = new InstanceSelectorNPC(this, golem, title, serverType, networkPlayerCountService);
        }
        registry.register(npc);
        return npc;
    }

    private NPC spawnStore(@NotNull Location location) {
        final IronGolem golem = generateDummyEntity(location);
        final StoreChestNPC npc = new StoreChestNPC(this, golem, cooldownManager, hub);
        registry.register(npc);
        return npc;
    }

    private NPC spawnTrainer(@NotNull Location location, @NotNull Location ffaSpawnpoint) {
        final IronGolem golem = generateDummyEntity(location);
        final TrainerNPC npc = new TrainerNPC(this, golem, cooldownManager, ffaSpawnpoint);
        registry.register(npc);
        return npc;
    }

    private NPC spawnComingSoon(@NotNull Location location) {
        final IronGolem golem = generateDummyEntity(location);
        final ComingSoonNPC npc = new ComingSoonNPC(this, golem);
        registry.register(npc);
        return npc;
    }

    private Entity spawnFfaArenaDisplay(@NotNull Location location) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setBillboard(Display.Billboard.FIXED);
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(2.5f),
                    new AxisAngle4f()
            ));
            display.text(Component.text("FFA Arena", TextColor.color(0xffbb00), TextDecoration.BOLD));
        });
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

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String name) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
