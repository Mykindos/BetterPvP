package me.mykindos.betterpvp.clans.world.camp;

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
 * What stands in a camp. Bound to the site rather than to a world name, so every clan's copy gets the same treatment
 * however many of them are open at once.
 * <p>
 * A dock and nothing else. A camp is somewhere a clan builds, so it is deliberately not put under the building rules
 * that a landmark is, and what a clan raises there is its own business.
 */
@Singleton
@PluginAdapter("Mapper")
public class CampContent {

    private final ClientManager clientManager;
    private final ClansSceneObjectFactory clansSceneFactory;

    @Inject
    private CampContent(@NotNull WorldContentService contentService, @NotNull WorldSites sites,
                        @NotNull ClientManager clientManager, @NotNull ClansSceneObjectFactory clansSceneFactory) {
        this.clientManager = clientManager;
        this.clansSceneFactory = clansSceneFactory;
        contentService.register(new WorldContentBinding(sites.selector(Camps.SITE_ID), this::content));
    }

    private @NotNull List<WorldContent> content() {
        return List.of(new Dock(clientManager, clansSceneFactory));
    }
}
