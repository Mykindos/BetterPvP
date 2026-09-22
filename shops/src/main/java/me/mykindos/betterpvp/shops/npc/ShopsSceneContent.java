package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.world.content.FactorySpawnPoints;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;

import java.util.List;

/**
 * Spawns shopkeeper NPCs in the main world at data-points named {@code shops:<type>}.
 */
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class ShopsSceneContent {

    @Inject
    public ShopsSceneContent(Shops plugin, ShopkeeperNPCFactory npcFactory, SceneObjectFactoryManager npcFactoryManager,
                             WorldContentService contentService) {
        // Registered so /npc spawn shops <type> works.
        npcFactoryManager.addObject("shops", npcFactory);
        final FactorySpawnPoints points = new FactorySpawnPoints("shops", npcFactory);
        contentService.register(plugin, new WorldContentBinding(WorldSelector.named(BPvPWorld.MAIN_WORLD_NAME),
                () -> List.of(points)).withRequiresModels(true));
    }
}
