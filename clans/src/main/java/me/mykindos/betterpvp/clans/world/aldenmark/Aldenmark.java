package me.mykindos.betterpvp.clans.world.aldenmark;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.clans.world.spawn.Spawn;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@PluginAdapter("Mapper")
public class Aldenmark extends Island {

    private final Clans clans;
    private final Dock dock;

    @Inject
    protected Aldenmark(@NotNull WorldContentService contentService, ClientManager clientManager,
                        ClansSceneObjectFactory clansSceneFactory, Spawn spawn, Clans clans) {
        super(contentService);
        this.clans = clans;
        this.dock = new Dock(clientManager, clansSceneFactory);
    }

    @Override
    public @NotNull String worldName() {
        // Read lazily (at load time) so the folder name is configurable without a redeploy.
        return clans.getConfig("config").getString("clans.aldenmark.world", "Aldenmark");
    }

    @Override
    public @NotNull List<WorldContent> content() {
        return List.of(this.dock);
    }

    @Override
    public @NotNull String name() {
        return "Aldenmark";
    }
}
