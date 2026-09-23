package me.mykindos.betterpvp.clans.world.camp.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import me.mykindos.betterpvp.core.world.schematic.BlockTransform;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * What a new camp starts with. A camp whose holding is still empty when its world opens is given one structure at
 * every {@code camp_start} marker the skin has: a perspective point tagged {@code structure:<id>}, facing the way the
 * structure should face. Something that starts broken, the Dock, is given broken.
 * <p>
 * Runs before the camp's structures are drawn, so the new ones are drawn with the rest.
 */
@Singleton
@CustomLog
public class StartingCamp implements WorldContent {

    public static final String MARKER = "camp_start";

    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final SchematicService schematics;

    @Inject
    public StartingCamp(@NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                        @NotNull SchematicService schematics) {
        this.construction = construction;
        this.catalogue = catalogue;
        this.schematics = schematics;
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        construction.worksite(world)
                .filter(worksite -> worksite.getHolding().getStructures().isEmpty())
                .ifPresent(worksite -> {
                    for (PerspectiveRegion marker : regions.find(MARKER, PerspectiveRegion.class)) {
                        final String id = RegionTags.of(marker).getString("structure", "");
                        final Optional<StructureType> type = catalogue.find(id);
                        if (type.isEmpty()) {
                            log.warn("A {} marker in '{}' names unknown structure '{}'", MARKER, world.getName(), id)
                                    .submit();
                            continue;
                        }
                        construction.grant(worksite, type.get(), position(type.get(), marker),
                                type.get().getFlags().isStartsBroken()
                                        ? StructureCondition.NEEDS_REPAIR : StructureCondition.ACTIVE);
                    }
                });
    }

    /** Where the marker puts the structure, turned from the way its build was saved to the way the marker faces. */
    private @NotNull StructurePosition position(@NotNull StructureType type, @NotNull PerspectiveRegion marker) {
        final float savedYaw = schematics.load(type.version(0).getSchematic())
                .map(schematic -> schematic.getAnchorYaw())
                .orElse(0f);
        return StructurePosition.of(marker.getLocation(),
                BlockTransform.quarterTurnsBetween(savedYaw, marker.getLocation().getYaw()));
    }
}
