package me.mykindos.betterpvp.clans.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCFactoryManager;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
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
public class ClansNPCs implements Listener, Reloadable {

    private final Clans plugin;
    private final NPCRegistry npcRegistry;
    private final NPCFactoryManager npcFactoryManager;
    private final ClansNPCFactory npcFactory;
    private boolean loaded;

    @Inject
    private ClansNPCs(Clans plugin, NPCRegistry npcRegistry, NPCFactoryManager npcFactoryManager, ClansNPCFactory npcFactory) {
        this.plugin = plugin;
        this.npcRegistry = npcRegistry;
        this.npcFactoryManager = npcFactoryManager;
        this.npcFactory = npcFactory;
    }

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

        for (NPC npc : this.npcRegistry.getNPCs(this.npcFactory)) {
            npc.remove();
        }

        this.npcFactoryManager.removeObject("clans");
        this.npcFactoryManager.addObject("clans", npcFactory);

        final World world = Objects.requireNonNull(Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME));
        final Iterator<Region> iterator = MapperHelper.getRegions(world).iterator();
        while (iterator.hasNext()) {
            Region region = iterator.next();

            if (!(region instanceof PerspectiveRegion perspectiveRegion)) {
                iterator.remove();
                continue;
            }

            String regionName = region.getName();
            if (!regionName.startsWith("clans:")) {
                continue;
            }

            String type = regionName.substring("clans:".length()).toLowerCase();
            perspectiveRegion.setWorld(world);
            final Location location = perspectiveRegion.getLocation();
            this.npcFactory.spawnDefault(location, type);
            iterator.remove();
        }
    }
}
