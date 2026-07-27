package me.mykindos.betterpvp.clans.world.aldenmark;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.clans.world.spawn.Spawn;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@PluginAdapter("Mapper")
public class Aldenmark extends Island {

    private final Dock dock;

    @Inject
    protected Aldenmark(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry, @NotNull SceneLoaderManager loaderManager,
                        @NotNull Clans clans, ClientManager clientManager, ClansSceneObjectFactory clansSceneFactory, Spawn spawn) {
        super(zoneManager, sceneRegistry, loaderManager, clans);
        this.dock = new Dock(clientManager, clansSceneFactory, List.of(spawn));
    }

    @Override
    public @NotNull List<WorldContent> content() {
        return List.of(this.dock);
    }

    @Override
    public @NotNull String name() {
        return BPvPWorld.MAIN_WORLD_NAME;
    }
}
