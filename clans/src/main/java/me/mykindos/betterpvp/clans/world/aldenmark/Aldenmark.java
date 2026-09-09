package me.mykindos.betterpvp.clans.world.aldenmark;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSites;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What stands on Aldenmark. Which world that is comes from the site catalogue, so the place and its contents are
 * configured in one file rather than two.
 */
@Singleton
@PluginAdapter("Mapper")
public class Aldenmark {

    private static final String SITE = "aldenmark";

    private final Dock dock;

    @Inject
    protected Aldenmark(@NotNull WorldContentService contentService, @NotNull WorldSites sites,
                        @NotNull ClientManager clientManager, @NotNull ClansSceneObjectFactory clansSceneFactory) {
        this.dock = new Dock(clientManager, clansSceneFactory);
        contentService.register(new WorldContentBinding(sites.selector(SITE), this::content));
    }

    private @NotNull List<WorldContent> content() {
        return List.of(this.dock);
    }
}
