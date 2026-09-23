package me.mykindos.betterpvp.clans.world.camp.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureFlags;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Every structure a camp can have, registered into the construction catalogue.
 * <p>
 * The Great Hall, Barracks and Dock are never demolished, and the Dock never moves because each skin decides where it
 * sits. The Dock starts broken, so repairing it is a new clan's first job.
 */
@Singleton
public class CampStructures {

    public static final String BARRACKS = "barracks";
    public static final String DOCK = "dock";
    public static final String WORKSHOP = "workshop";
    public static final String STOREHOUSE = "storehouse";

    private final List<CampStructure> structures;

    @Inject
    public CampStructures(@NotNull CampConfig config, @NotNull StructureCatalogue catalogue) {
        final StructureFlags permanent = StructureFlags.builder().demolishable(false).build();
        final StructureFlags ordinary = StructureFlags.builder().build();
        final Set<String> afterHall = Set.of(CampConstruction.GREAT_HALL);

        this.structures = List.of(
                new CampStructure(CampConstruction.GREAT_HALL, 1, Set.of(), null, permanent, config),
                new CampStructure(BARRACKS, 1, afterHall, null, permanent, config),
                new CampStructure(DOCK, 1, afterHall, null,
                        StructureFlags.builder().demolishable(false).movable(false).startsBroken(true).build(), config),
                new CampStructure(WORKSHOP, 1, afterHall, null, ordinary, config),
                new CampStructure(STOREHOUSE, 1, afterHall, null, ordinary, config));
        structures.forEach(catalogue::register);
    }

    public @NotNull Collection<CampStructure> all() {
        return structures;
    }
}
