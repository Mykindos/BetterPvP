package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.configuration.WorldConfiguration;
import io.papermc.paper.configuration.type.number.DoubleOr;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.events.kill.PlayerSuicideEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.spigotmc.SpigotConfig;

import java.util.EnumSet;
import java.util.OptionalDouble;
import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class CoreWorldListener implements Listener {

    private final ClientManager clientManager;
    private final WorldHandler worldHandler;

    @Inject
    public CoreWorldListener(ClientManager clientManager, WorldHandler worldHandler) {
        this.clientManager = clientManager;
        this.worldHandler = worldHandler;
        ((CraftServer) Bukkit.getServer()).getServer().setFlightAllowed(true);


        GlobalConfiguration paperConfig = GlobalConfiguration.get();

        paperConfig.collisions.enablePlayerCollisions = false;
        paperConfig.scoreboards.saveEmptyScoreboardTeams = false;
        paperConfig.misc.clientInteractionLeniencyDistance = new DoubleOr.Default(OptionalDouble.of(3.0));

        SpigotConfig.maxHealth = 10000.0;
        ((RangedAttribute) Attributes.MAX_HEALTH.value()).maxValue = 10000.0;

    }

    @EventHandler
    public void onLoadWorld(WorldLoadEvent event) {
        World world = event.getWorld();
        if(world.getName().equals("world")) {
            worldHandler.loadSpawnLocations();
        }

        var paperConfig = ((CraftWorld) world).getHandle().getLevel().paperConfig();
        paperConfig.misc.disableRelativeProjectileVelocity = true;
        paperConfig.misc.redstoneImplementation = WorldConfiguration.Misc.RedstoneImplementation.ALTERNATE_CURRENT;
        paperConfig.entities.behavior.allowSpiderWorldBorderClimbing = false;
        paperConfig.entities.behavior.disableChestCatDetection = true;
        paperConfig.entities.behavior.disableCreeperLingeringEffect = true;
        paperConfig.entities.behavior.disablePlayerCrits = true;
        paperConfig.collisions.maxEntityCollisions = 4;
        paperConfig.chunks.maxAutoSaveChunksPerTick = 8;
        paperConfig.environment.optimizeExplosions = true;
        paperConfig.scoreboards.allowNonPlayerEntitiesOnScoreboards = false;
        paperConfig.tickRates.containerUpdate = 2;
        paperConfig.tickRates.grassSpread = 4;
        paperConfig.entities.spawning.perPlayerMobSpawns = true;



        var spigotConfig = ((CraftWorld) world).getHandle().getLevel().spigotConfig;
        spigotConfig.animalTrackingRange = 64;
        spigotConfig.monsterTrackingRange = 64;
        spigotConfig.playerTrackingRange = 96;
        spigotConfig.displayTrackingRange = 96;
        spigotConfig.mobSpawnRange = 6;

        world.setViewDistance(7);
        world.setSimulationDistance(world.getViewDistance() - 1);

        world.setSpawnLimit(SpawnCategory.MONSTER, 8);
        world.setSpawnLimit(SpawnCategory.ANIMAL, 4);
        world.setSpawnLimit(SpawnCategory.WATER_AMBIENT, 1);
        world.setSpawnLimit(SpawnCategory.WATER_ANIMAL,1);
        world.setSpawnLimit(SpawnCategory.WATER_UNDERGROUND_CREATURE, 1);
        world.setSpawnLimit(SpawnCategory.AMBIENT, 1);
        world.setSpawnLimit(SpawnCategory.AXOLOTL, 1);

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

    private final static Set<TreeType> BIG_TREES = EnumSet.of(
            TreeType.BIG_TREE,
            TreeType.TALL_BIRCH,
            TreeType.TALL_REDWOOD,
            TreeType.MEGA_REDWOOD,
            TreeType.MEGA_PINE,
            TreeType.RED_MUSHROOM,
            TreeType.BROWN_MUSHROOM
    );

    @EventHandler
    public void onTreeGrow(StructureGrowEvent event) {
        if(BIG_TREES.contains(event.getSpecies())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSaplingDrop(ItemSpawnEvent event) {
        if(event.getEntity().getItemStack().getType() == Material.CHERRY_SAPLING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        UtilBlock.WEAK_BLOCKMAP_CACHE.invalidate(event.getChunk());
    }

    @EventHandler (ignoreCancelled = true)
    public void onInteractInvisArmorStand(PlayerInteractEntityEvent event) {
        if(event.getRightClicked() instanceof ArmorStand){
            ArmorStand armorStand = (ArmorStand) event.getRightClicked();
            if(armorStand.isInvisible()){
                event.setCancelled(true);
            }
        }
    }
}
