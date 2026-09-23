package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.core.world.construction.BuildZones;
import me.mykindos.betterpvp.core.world.construction.view.StructureViews;
import me.mykindos.betterpvp.core.world.settler.presence.SettlerPresence;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSites;
import me.mykindos.betterpvp.clans.world.camp.hall.Steward;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.clans.world.camp.settler.StarterCrew;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.StartingCamp;
import me.mykindos.betterpvp.clans.world.model.Dock;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What stands in a camp. Bound to the site rather than to a world name, so every clan's copy gets the same treatment
 * however many of them are open at once.
 * <p>
 * A dock, the build zones the skin marks out, whatever structures the clan has raised, and its settlers. A camp is
 * somewhere a clan builds, so it is deliberately not put under the building rules that a landmark is.
 */
@Singleton
@PluginAdapter("Mapper")
public class CampContent {

    private final ClientManager clientManager;
    private final ClansSceneObjectFactory clansSceneFactory;
    private final WorldContent structures;
    private final WorldContent settlers;
    private final StarterCrew starterCrew;
    private final WorldContent steward;
    private final WorldContent buildZones = new BuildZones();
    private final StartingCamp startingCamp;
    private final CampGrounds grounds;

    @Inject
    private CampContent(@NotNull WorldContentService contentService, @NotNull Clans clans, @NotNull WorldSites sites,
                        @NotNull ClientManager clientManager, @NotNull ClansSceneObjectFactory clansSceneFactory,
                        @NotNull StructureViews views, @NotNull CampConstruction construction,
                        @NotNull CampStructures campStructures, @NotNull StartingCamp startingCamp,
                        @NotNull CampGrounds grounds, @NotNull SettlerPresence settlerPresence,
                        @NotNull StarterCrew starterCrew, @NotNull Steward steward) {
        this.clientManager = clientManager;
        this.clansSceneFactory = clansSceneFactory;
        this.structures = views.content();
        this.settlers = settlerPresence.content();
        this.starterCrew = starterCrew;
        this.steward = steward.content();
        this.startingCamp = startingCamp;
        this.grounds = grounds;
        contentService.register(clans, new WorldContentBinding(sites.selector(Camps.SITE_ID), this::content));
    }

    private @NotNull List<WorldContent> content() {
        return List.of(grounds, new Dock(clientManager, clansSceneFactory, false), buildZones, startingCamp, starterCrew,
                structures, settlers, steward);
    }
}
