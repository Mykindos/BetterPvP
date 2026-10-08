package me.mykindos.betterpvp.core.world.construction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureConditionTest {

    @Test
    void ac2_aStructureStandsOnceBuiltAndWhilePlaced() {
        assertTrue(StructureCondition.ACTIVE.isStanding());
        assertTrue(StructureCondition.DISABLED.isStanding());
        assertTrue(StructureCondition.NEEDS_REPAIR.isStanding());
        assertFalse(StructureCondition.UNDER_CONSTRUCTION.isStanding());
        assertFalse(StructureCondition.NOT_PLACED.isStanding());
    }
}
