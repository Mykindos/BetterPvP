package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCFactoryManager;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.shops.Shops;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.Objects;

@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
@BPvPListener
@Singleton
@CustomLog
public class ShopsNPCs implements Listener, Reloadable {

    private final Shops plugin;
    private final NPCRegistry npcRegistry;
    private final NPCFactoryManager npcFactoryManager;
    private final ShopkeeperNPCFactory npcFactory;
    private boolean loaded;

    @Inject
    private ShopsNPCs(Shops plugin, NPCRegistry npcRegistry, NPCFactoryManager npcFactoryManager, ShopkeeperNPCFactory npcFactory) {
        this.plugin = plugin;
        this.npcRegistry = npcRegistry;
        this.npcFactoryManager = npcFactoryManager;
        this.npcFactory = npcFactory;
    }

    /**
     * This prevents shops from loading before ModelEngine loads the models, which would shutdown the onEnable phase of
     * {@link Shops}
     */
    @EventHandler
    void onLoad(ModelRegistrationEvent event) {
        if (event.getPhase() != ModelGenerator.Phase.FINISHED) {
            return;
        }

        UtilServer.runTask(plugin, () -> {
            this.loaded = true;
            reload();
        });
    }

    @Override
    public void reload() {
        if (!loaded) {
            return;
        }

        // Despawn old NPCs
        log.info("Despawning old Shops NPCs").submit();
        for (NPC npc : this.npcRegistry.getNPCs(this.npcFactory)) {
            npc.remove();
        }

        // Remove factory
        log.info("Registering Shops NPC factory").submit();
        this.npcFactoryManager.removeObject("shops");
        this.npcFactoryManager.addObject("shops", npcFactory);

        // Then respawn NPCs
        log.info("Respawning Shops NPCs").submit();
        final World world = Objects.requireNonNull(Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME));
        final Iterator<Region> iterator = MapperHelper.getRegions(world).iterator();
        while (iterator.hasNext()) {
            Region region = iterator.next();

            if (!(region instanceof PerspectiveRegion perspectiveRegion)) {
                log.warn("Region {} is not a PerspectiveRegion, skipping", region.getName()).submit();
                iterator.remove();
                continue;
            }

            String regionName = region.getName();
            if (!regionName.startsWith("shops:")) {
                continue;
            }

            String type = regionName.substring("shops:".length()).toLowerCase();
            perspectiveRegion.setWorld(world);
            final Location location = perspectiveRegion.getLocation();
            final NPC npc = this.npcFactory.spawnDefault(location, type);
            this.npcRegistry.register(npc);
            iterator.remove();

            log.info("Spawned {} shopkeeper at ({}, {}, {})",
                    type,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()).submit();
        }
    }

}
