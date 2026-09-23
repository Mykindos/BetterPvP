package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Everything settlers need from a camp: the roster is kept on the camp record, the Great Hall's stage decides how many
 * settlers it can have and how many of each profession can work, and structures such as the Workshop add to that.
 */
@Singleton
public class CampSettlers implements SettlerSite {

    private final CampStore store;
    private final SettlerConfig config;

    @Inject
    public CampSettlers(@NotNull CampStore store, @NotNull SettlerConfig config, @NotNull SettlerService service,
                        @NotNull CampProfessions professions, @NotNull CampTraits traits) {
        this.store = store;
        this.config = config;
        service.register(Camps.SITE_ID, this);
    }

    @Override
    public @NotNull Optional<Roster> roster(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId()).map(Camp::getRoster);
    }

    @Override
    public void changed(@NotNull SiteKey site) {
        store.changed(site.getOwnerId());
    }

    @Override
    public int populationCap(@NotNull SiteKey site) {
        return config.population(hallStage(site));
    }

    @Override
    public @NotNull OptionalInt workingCap(@NotNull SiteKey site, @NotNull String profession) {
        return config.workingCap(profession).map(cap -> {
            final Holding holding = store.cached(site.getOwnerId()).map(Camp::getHolding).orElseGet(Holding::new);
            int total = SettlerConfig.byStage(cap.getByHallStage(), finishedStage(holding, CampConstruction.GREAT_HALL));
            for (Map.Entry<String, Integer> bonus : cap.getPerStageOf().entrySet()) {
                total += (finishedStage(holding, bonus.getKey()) + 1) * bonus.getValue();
            }
            return OptionalInt.of(total);
        }).orElseGet(OptionalInt::empty);
    }

    private int hallStage(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId())
                .map(camp -> finishedStage(camp.getHolding(), CampConstruction.GREAT_HALL))
                .orElse(-1);
    }

    /** The highest stage a finished structure of {@code type} stands at, counting from 0, or -1 for none. */
    static int finishedStage(@NotNull Holding holding, @NotNull String type) {
        return holding.ofType(type).stream()
                .filter(structure -> structure.getCondition() != StructureCondition.UNDER_CONSTRUCTION
                        && structure.getCondition() != StructureCondition.NOT_PLACED)
                .mapToInt(PlacedStructure::getStage)
                .max()
                .orElse(-1);
    }
}
