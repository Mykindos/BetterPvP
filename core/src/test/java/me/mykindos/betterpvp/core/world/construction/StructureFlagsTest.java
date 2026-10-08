package me.mykindos.betterpvp.core.world.construction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureFlagsTest {

    @Test
    void ac2_aStructureStartsActive() {
        assertEquals(StructureCondition.ACTIVE, StructureFlags.builder().build().initialCondition());
    }

    @Test
    void ac2_aStructureThatStartsBrokenNeedsARepair() {
        assertEquals(StructureCondition.NEEDS_REPAIR,
                StructureFlags.builder().startsBroken(true).build().initialCondition());
    }
}
