package me.mykindos.betterpvp.clans.world.camp.upgrade;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPiece;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopPagesTest {

    private static final long MINUTE = 60_000;

    @Test
    void theJobBoardListsReadyThenRunningSoonestFirstThenPaused() {
        final Holding holding = new Holding();
        final PlacedStructure idle = structure(holding, null);
        final PlacedStructure slow = structure(holding, Job.start(JobKind.ADVANCE, Duration.ofMinutes(30),
                ResourceCost.NONE, 1, 0));
        final Job heldJob = Job.start(JobKind.REPAIR, Duration.ofMinutes(5), ResourceCost.NONE, 0, 0);
        heldJob.hold("crew", 0);
        final PlacedStructure paused = structure(holding, heldJob);
        final PlacedStructure quick = structure(holding, Job.start(JobKind.FIT_UPGRADE, Duration.ofMinutes(10),
                ResourceCost.NONE, 0, 0));
        final PlacedStructure ready = structure(holding, Job.start(JobKind.BUILD, Duration.ofMinutes(1),
                ResourceCost.NONE, 0, 0));

        final List<JobBoard.Entry> entries = JobBoard.entries(holding, 2 * MINUTE);

        assertEquals(List.of(ready, quick, slow, paused), entries.stream().map(JobBoard.Entry::getStructure).toList());
        assertFalse(entries.stream().anyMatch(entry -> entry.getStructure() == idle), "no job, no entry");
        assertEquals(JobBoard.State.READY, entries.get(0).getState());
        assertEquals(8 * MINUTE, entries.get(1).getRemainingMillis());
        assertEquals(JobBoard.State.PAUSED, entries.get(3).getState());
    }

    @Test
    void aGhostPieceFitsOnlyIfNoColumnUnderItClashes() {
        final LongSet clashes = LongSet.of(Footprint.pack(12, 5));
        final GhostPiece box = new GhostPiece(null, 0, 0, 0, 2, 1, 2);

        assertTrue(SurveyorsTable.fits(clashes, 0, 0, box));
        assertFalse(SurveyorsTable.fits(clashes, 11, 4, box), "covers 11..12 by 4..5");
        assertTrue(SurveyorsTable.fits(clashes, 13, 4, box));
        assertFalse(SurveyorsTable.fits(clashes, 12, 5, new GhostPiece(null, 0, 3, 0, 1, 1, 1)),
                "height does not matter, only the column");
    }

    private static PlacedStructure structure(Holding holding, Job job) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), CampStructures.WORKSHOP,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        structure.setJob(job);
        holding.getStructures().add(structure);
        return structure;
    }
}
