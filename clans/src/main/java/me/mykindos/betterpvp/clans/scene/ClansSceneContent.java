package me.mykindos.betterpvp.clans.scene;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.world.content.FactorySpawnPoints;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;

import java.util.List;

/**
 * Spawns clans NPCs in the main world at data-points named {@code clans:<type>} (e.g. {@code clans:traveler}).
 */
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class ClansSceneContent {

    @Inject
    public ClansSceneContent(Clans clans, ClansSceneObjectFactory npcFactory, SceneObjectFactoryManager npcFactoryManager,
                             WorldContentService contentService) {
        // Registered so /npc spawn clans <type> works.
        npcFactoryManager.addObject("clans", npcFactory);
        final FactorySpawnPoints points = new FactorySpawnPoints("clans", npcFactory);
        contentService.register(clans, new WorldContentBinding(WorldSelector.named(BPvPWorld.MAIN_WORLD_NAME),
                () -> List.of(points)).withRequiresModels(true));
    }
}
