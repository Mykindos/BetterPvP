package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.MapperSceneLoader;
import me.mykindos.betterpvp.core.scene.loader.ModelEngineLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.npc.NPCFactoryManager;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.shops.Shops;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Loads all shops NPCs from Mapper data-points whenever ModelEngine finalizes its pipeline.
 * <p>
 * Data-points must be named {@code shops:<type>} (e.g. {@code shops:fisherman}).
 * Replaces the old {@code ShopsNPCs} listener which manually wired {@link com.ticxo.modelengine.api.events.ModelRegistrationEvent}.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class ShopsSceneLoader extends MapperSceneLoader {

    private final ShopkeeperNPCFactory npcFactory;
    private final NPCFactoryManager npcFactoryManager;

    @Inject
    public ShopsSceneLoader(Shops shops, ShopkeeperNPCFactory npcFactory, NPCFactoryManager npcFactoryManager, SceneLoaderManager sceneLoaderManager) {
        this.npcFactory = npcFactory;
        this.npcFactoryManager = npcFactoryManager;
        sceneLoaderManager.register(this, shops);
    }

    @Override
    @NotNull
    protected Collection<Region> getRegions() {
        final World world = Objects.requireNonNull(Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME));
        return MapperHelper.getRegions(world);
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ModelEngineLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        // Keep the factory registered so /npc spawn shops <type> keeps working.
        npcFactoryManager.removeObject("shops");
        npcFactoryManager.addObject("shops", npcFactory);

        final World world = Objects.requireNonNull(Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME));
        int spawned = 0;
        for (Region region : getRegions()) {
            if (!(region instanceof PerspectiveRegion perspectiveRegion)) continue;

            final String regionName = region.getName();
            if (!regionName.startsWith("shops:")) continue;

            final String type = regionName.substring("shops:".length()).toLowerCase();
            perspectiveRegion.setWorld(world);

            track(npcFactory.spawnDefault(perspectiveRegion.getLocation(), type));
            spawned++;
        }

        log.info("Spawned {} shops NPC(s)", spawned).submit();
    }
}
