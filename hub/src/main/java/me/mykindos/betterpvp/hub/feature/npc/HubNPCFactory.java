package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.ServerType;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.model.HubWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class HubNPCFactory extends NPCFactory implements ConfigAccessor {

    private final List<NPC> npcs = new ArrayList<>();
    private final CooldownManager cooldownManager;
    private final Hub hub;
    private final HubWorld hubWorld;

    @Inject
    private HubNPCFactory(NPCRegistry registry, CooldownManager cooldownManager, Hub hub, HubWorld hubWorld) {
        super("hub", registry);
        this.cooldownManager = cooldownManager;
        this.hub = hub;
        this.hubWorld = hubWorld;
    }

    public boolean tryLoad(Hub hub) {
        final Set<String> keys = ModelEngineAPI.getAPI().getModelRegistry().getKeys();
        if (!keys.contains("dummy") || !keys.contains("chest_shadow")) {
            return false;
        }

        loadConfig(hub.getConfig("datapoints"));
        return true;
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        for (NPC npc : npcs) {
            npc.remove();
        }

        final World world = hubWorld.getWorld();
        final Location storeLocation = getDataPoint("npc_store", PerspectiveRegion.class).getLocation();
        storeLocation.setWorld(world);
        this.npcs.add(this.spawnStore(storeLocation));

        final Location trainerLocation = getDataPoint("npc_trainer", PerspectiveRegion.class).getLocation();
        trainerLocation.setWorld(world);
        final Location ffaSpawnpoint = getDataPoint("ffa_spawnpoint", PerspectiveRegion.class).getLocation();
        this.npcs.add(this.spawnTrainer(trainerLocation, ffaSpawnpoint));

        final Location classicLocation = getDataPoint("npc_selector_classic", PerspectiveRegion.class).getLocation();
        classicLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(classicLocation, ServerType.CLANS_CLASSIC, false));

        final Location squadsLocation = getDataPoint("npc_selector_squads", PerspectiveRegion.class).getLocation();
        squadsLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(squadsLocation, ServerType.CLANS_SQUADS, false));

        final Location casualLocation = getDataPoint("npc_selector_casual", PerspectiveRegion.class).getLocation();
        casualLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(casualLocation, ServerType.CLANS_CASUAL, true));
    }

    private <T extends Region> T getDataPoint(String name, Class<T> type) {
        return this.hubWorld.getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No region found with name: " + name));
    }

    private NPC spawnInstanceSelector(@NotNull Location location, ServerType serverType, boolean featured) {
        final IronGolem golem = generateDummyEntity(location);
        final TextComponent title = switch (serverType) {
            case CLANS_CLASSIC -> Component.text("Classic", NamedTextColor.RED, TextDecoration.BOLD);
            case CLANS_SQUADS -> Component.text("Squads", NamedTextColor.AQUA, TextDecoration.BOLD);
            case CLANS_CASUAL -> Component.text("Casual", NamedTextColor.GREEN, TextDecoration.BOLD);
            default -> throw new IllegalArgumentException("Unexpected value: " + serverType);
        };

        final NPC npc;
        if (featured) {
            final TextColor[] gradient = new TextColor[]{
                    TextColor.color(255, 226, 36),
                    TextColor.color(255, 182, 23),
                    TextColor.color(255, 116, 23),
            };
            npc = new InstanceSelectorNPC.Featured(this, golem, title.content(), gradient, serverType);
        } else {
            npc = new InstanceSelectorNPC(this, golem, title, serverType);
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
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class);
        golem.setAI(false);
        golem.setInvulnerable(true);
        golem.setPersistent(false);
        golem.setCollidable(false);
        golem.setInvisible(true);
        final TrainerNPC npc = new TrainerNPC(this, golem, cooldownManager, ffaSpawnpoint);
        registry.register(npc);
        return npc;
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
