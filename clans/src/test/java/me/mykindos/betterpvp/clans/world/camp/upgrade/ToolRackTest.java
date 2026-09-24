package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolRackTest {

    private static final long CLAN = 3;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private ToolRack rack;
    private PlacedStructure workshop;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        rack = new ToolRack(new CampUpgrades(store));
        workshop = new PlacedStructure(UUID.randomUUID(), CampStructures.WORKSHOP, new StructurePosition(0, 64, 0, 0),
                StructureCondition.ACTIVE);
        camp.getHolding().getStructures().add(workshop);
    }

    @Test
    void withoutTheRackNothingChanges() {
        final BuilderStats laborer = stats(CampProfessions.LABORER, Set.of(CampProfessions.CARPENTER));

        assertSame(laborer, rack.apply(SITE, laborer));
    }

    @Test
    void theRackMakesLaborersCompatibleWithEveryTradeBothWays() {
        workshop.getUpgrades().put(0, ToolRack.ID);

        assertEquals(Set.of(CampProfessions.MASON, CampProfessions.CARPENTER, CampProfessions.SMITH),
                rack.apply(SITE, stats(CampProfessions.LABORER, Set.of(CampProfessions.CARPENTER))).getCompatible());
        assertEquals(Set.of(CampProfessions.MASON, CampProfessions.LABORER),
                rack.apply(SITE, stats(CampProfessions.SMITH, Set.of(CampProfessions.MASON))).getCompatible());
    }

    @Test
    void aDisabledWorkshopLosesTheRack() {
        workshop.getUpgrades().put(0, ToolRack.ID);
        workshop.setCondition(StructureCondition.DISABLED);
        final BuilderStats laborer = stats(CampProfessions.LABORER, Set.of(CampProfessions.CARPENTER));

        assertSame(laborer, rack.apply(SITE, laborer));
    }

    private static BuilderStats stats(String trade, Set<String> compatible) {
        return new BuilderStats(2, 1.0, 0.5, trade, compatible);
    }
}
