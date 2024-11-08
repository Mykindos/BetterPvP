package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.configuration.WorldConfiguration;
import io.papermc.paper.configuration.type.number.DoubleOr;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.events.kill.PlayerSuicideEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.spigotmc.SpigotConfig;

import java.util.OptionalDouble;

@BPvPListener
@Singleton
public class CoreWorldListener implements Listener {

    private final ClientManager clientManager;
    private final WorldHandler worldHandler;

    @Inject
    public CoreWorldListener(ClientManager clientManager, WorldHandler worldHandler) {
        this.clientManager = clientManager;
        this.worldHandler = worldHandler;
        ((CraftServer) Bukkit.getServer()).getServer().setFlightAllowed(true);

        GlobalConfiguration.get().collisions.enablePlayerCollisions = false;
        GlobalConfiguration.get().scoreboards.saveEmptyScoreboardTeams = false;
        GlobalConfiguration.get().misc.clientInteractionLeniencyDistance = new DoubleOr.Default(OptionalDouble.of(3.0));

        SpigotConfig.maxHealth = 10000.0;
        ((RangedAttribute) Attributes.MAX_HEALTH.value()).maxValue = 10000.0;


    }

    @EventHandler
    public void onLoadWorld(WorldLoadEvent event) {
        if(event.getWorld().getName().equals("world")) {
            worldHandler.loadSpawnLocations();
        }

        var paperConfig = ((CraftWorld) event.getWorld()).getHandle().getLevel().paperConfig();
        paperConfig.misc.disableRelativeProjectileVelocity = true;
        paperConfig.misc.redstoneImplementation = WorldConfiguration.Misc.RedstoneImplementation.ALTERNATE_CURRENT;
        paperConfig.entities.behavior.allowSpiderWorldBorderClimbing = false;
        paperConfig.entities.behavior.disableChestCatDetection = true;
        paperConfig.entities.behavior.disableCreeperLingeringEffect = true;
        paperConfig.entities.behavior.disablePlayerCrits = true;
        paperConfig.collisions.maxEntityCollisions = 4;
        paperConfig.chunks.maxAutoSaveChunksPerTick = 8;
        paperConfig.environment.optimizeExplosions = true;
        paperConfig.hopper.disableMoveEvent = true;
        paperConfig.scoreboards.allowNonPlayerEntitiesOnScoreboards = false;
        paperConfig.tickRates.containerUpdate = 2;
        paperConfig.tickRates.grassSpread = 4;
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.AMBIENT, 1);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.AXOLOTLS, 1);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.MONSTER, 20);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.CREATURE, 10);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.WATER_AMBIENT, 1);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.WATER_CREATURE, 1);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.MISC, 1);
        paperConfig.entities.spawning.spawnLimits.put(MobCategory.UNDERGROUND_WATER_CREATURE, 1);

        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.AMBIENT, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.AXOLOTLS, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.MONSTER, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.CREATURE, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.WATER_AMBIENT, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.WATER_CREATURE, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.MISC, 400);
        paperConfig.entities.spawning.ticksPerSpawn.put(MobCategory.UNDERGROUND_WATER_CREATURE, 400);

        var spigotConfig = ((CraftWorld) event.getWorld()).getHandle().getLevel().spigotConfig;
        spigotConfig.animalTrackingRange = 64;
        spigotConfig.monsterTrackingRange = 64;
        spigotConfig.playerTrackingRange = 96;
        spigotConfig.displayTrackingRange = 96;
        spigotConfig.mobSpawnRange = 3;

    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(worldHandler.getSpawnLocation());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(!event.getPlayer().hasPlayedBefore()) {
            event.getPlayer().teleport(worldHandler.getSpawnLocation());
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onSuicide(PlayerSuicideEvent event) {
        if(event.isCancelled()) return;

        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);

        if (client.hasRank(Rank.ADMIN)) {
            event.setDelayInSeconds(0);
            return;
        }

        if (client.getGamer().isInCombat()) {
            event.setDelayInSeconds(15);
        }

    }
}
