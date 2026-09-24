package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.RecruitConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarbourmasterTest {

    private static final long CLAN = 7;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long HOUR = 3_600_000L;

    private final AtomicLong now = new AtomicLong(500 * HOUR);
    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final CampPermissions permissions = mock(CampPermissions.class);
    private final CampRecruitment recruitment = mock(CampRecruitment.class);
    private final RecruitConfig recruitConfig = mock(RecruitConfig.class);
    private final Player player = mock(Player.class);
    private final SettlerCandidate rolled = new SettlerCandidate(new Settler(), 0, 0);
    private Harbourmaster harbourmaster;
    private PlacedStructure dock;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(true);
        when(recruitment.boat(any(), any())).thenReturn(List.of(rolled));
        when(recruitConfig.getArrivalOdds()).thenReturn(new SettlerOdds(Map.of(),
                Map.of(CampProfessions.BUILDER, 40.0, CampProfessions.FARMER, 20.0, SettlerOdds.NONE, 40.0)));
        harbourmaster = new Harbourmaster(store, new CampUpgrades(store), mock(CampConfig.class), permissions,
                recruitment, recruitConfig, now::get);
        dock = new PlacedStructure(UUID.randomUUID(), CampStructures.DOCK,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        dock.getUpgrades().put(0, Harbourmaster.ID);
        camp.getHolding().getStructures().add(dock);
    }

    @Test
    void lookingRollsTheNextBoatAhead() {
        assertNull(harbourmaster.look(player, SITE));

        assertEquals(List.of(rolled), harbourmaster.foreseen(SITE));
        assertEquals(now.get(), camp.getHarbourmasterUsedAt());
    }

    @Test
    void itCanBeUsedOncePerDay() {
        assertNull(harbourmaster.look(player, SITE));
        now.addAndGet(23 * HOUR);
        assertEquals("clans.camp.upgrade.harbourmaster.spent",
                harbourmaster.choose(player, SITE, CampProfessions.FARMER));

        now.addAndGet(HOUR);
        assertNull(harbourmaster.choose(player, SITE, CampProfessions.FARMER));
        assertEquals(CampProfessions.FARMER, harbourmaster.chosen(SITE));
        assertTrue(harbourmaster.foreseen(SITE).isEmpty(), "choosing replaces the boat that was seen");
    }

    @Test
    void onlyProfessionsBoatsBringCanBeChosen() {
        assertEquals(List.of(CampProfessions.BUILDER, CampProfessions.FARMER), harbourmaster.choices());
        assertEquals("clans.camp.upgrade.harbourmaster.unknown", harbourmaster.choose(player, SITE, SettlerOdds.NONE));
        assertEquals(0, camp.getHarbourmasterUsedAt());
    }

    @Test
    void aDisabledDockHasNoHarbourmaster() {
        dock.setCondition(StructureCondition.DISABLED);
        assertEquals("clans.camp.upgrade.harbourmaster.inactive", harbourmaster.look(player, SITE));
    }

    @Test
    void itNeedsTheRightToHire() {
        when(permissions.allows(any(Player.class), eq(CLAN), eq(SettlerAction.HIRE))).thenReturn(false);
        assertEquals("clans.settler.card.not_allowed", harbourmaster.look(player, SITE));
    }
}
