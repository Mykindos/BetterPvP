package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.world.schematic.Footprint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeputyStewardTest {

    @Test
    void aClearSpotInTheCampIsFine() {
        final Footprint hall = footprint(0, 0);

        assertNull(DeputySteward.placementProblem(true, true, List.of(hall), 10, 10));
    }

    @Test
    void anotherCampIsRefused() {
        assertEquals("clans.camp.upgrade.deputy_steward.not_here",
                DeputySteward.placementProblem(false, true, List.of(), 0, 0));
    }

    @Test
    void itNeedsGroundToStandOn() {
        assertEquals("clans.camp.upgrade.deputy_steward.no_ground",
                DeputySteward.placementProblem(true, false, List.of(), 0, 0));
    }

    @Test
    void aStructureFootprintIsRefused() {
        final Footprint hall = footprint(4, 5);

        assertEquals("clans.camp.upgrade.deputy_steward.in_structure",
                DeputySteward.placementProblem(true, true, List.of(footprint(0, 0), hall), 4, 5));
    }

    private static Footprint footprint(int x, int z) {
        final Footprint footprint = mock(Footprint.class);
        when(footprint.containsColumn(x, z)).thenReturn(true);
        return footprint;
    }
}
