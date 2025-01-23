package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.ServerType;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.hub.Hub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Singleton
public class HubNPCFactory extends NPCFactory implements ConfigAccessor {

    private final List<NPC> npcs = new ArrayList<>();
    private final CooldownManager cooldownManager;
    private final Hub hub;

    @Inject
    private HubNPCFactory(NPCRegistry registry, CooldownManager cooldownManager, Hub hub) {
        super("hub", registry);
        this.cooldownManager = cooldownManager;
        this.hub = hub;
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

        final World world = Bukkit.getWorld("world");
        final Location storeLocation = Objects.requireNonNull(config.getLocation("npc.store"), "Store location is not set");
        storeLocation.setWorld(world);
        this.npcs.add(this.spawnStore(storeLocation));

        final Location trainerLocation = Objects.requireNonNull(config.getLocation("npc.trainer"), "Trainer location is not set");
        trainerLocation.setWorld(world);
        this.npcs.add(this.spawnTrainer(trainerLocation));

        final Location classicLocation = Objects.requireNonNull(config.getLocation("npc.classic"), "Classic Selector location is not set");
        classicLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(classicLocation, ServerType.CLANS_CLASSIC, false));

        final Location squadsLocation = Objects.requireNonNull(config.getLocation("npc.squads"), "Squads Selector location is not set");
        squadsLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(squadsLocation, ServerType.CLANS_SQUADS, false));

        final Location casualLocation = Objects.requireNonNull(config.getLocation("npc.casual"), "Casual Selector location is not set");
        casualLocation.setWorld(world);
        this.npcs.add(this.spawnInstanceSelector(casualLocation, ServerType.CLANS_CASUAL, true));
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

    private NPC spawnTrainer(@NotNull Location location) {
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class);
        golem.setAI(false);
        golem.setInvulnerable(true);
        golem.setPersistent(false);
        golem.setCollidable(false);
        golem.setInvisible(true);
        final TrainerNPC npc = new TrainerNPC(this, golem, cooldownManager);
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
