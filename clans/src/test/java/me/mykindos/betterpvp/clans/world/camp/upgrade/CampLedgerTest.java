package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.WageFundPaidEvent;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampLedgerTest {

    private static final long CLAN = 5;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final Player player = mock(Player.class);
    private final AtomicLong now = new AtomicLong(1_000);
    private CampLedger ledger;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Steve");
        ledger = new CampLedger(store, new CampUpgrades(store), mock(CampConfig.class), mock(CampStructures.class),
                now::get);
    }

    @Test
    void appendDropsTheOldestPastTheLimit() {
        final List<LedgerEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CampLedger.append(entries, entry(i), 3);
        }

        assertEquals(List.of(2L, 3L, 4L), entries.stream().map(LedgerEntry::getAt).toList());
    }

    @Test
    void newestFirstReversesWithoutTouchingTheRecord() {
        final List<LedgerEntry> entries = new ArrayList<>(List.of(entry(1), entry(2), entry(3)));

        assertEquals(List.of(3L, 2L, 1L), CampLedger.newestFirst(entries).stream().map(LedgerEntry::getAt).toList());
        assertEquals(1L, entries.getFirst().getAt());
    }

    @Test
    void paymentsAreRecordedEvenWithoutTheLedgerFitted() {
        ledger.onWages(new WageFundPaidEvent(SITE, player, 250));

        assertEquals(1, camp.getLedger().size());
        final LedgerEntry entry = camp.getLedger().getFirst();
        assertEquals(LedgerEntry.Kind.WAGES, entry.getKind());
        assertEquals(List.of("250"), entry.getArgs());
        assertEquals("Steve", entry.getMemberName());
        assertEquals(1_000, entry.getAt());
        verify(store).changed(CLAN);
    }

    @Test
    void theCampKeepsTheDefaultTwoHundredNewest() {
        for (int i = 0; i < 205; i++) {
            now.set(i);
            ledger.onWages(new WageFundPaidEvent(SITE, player, i));
        }

        assertEquals(200, camp.getLedger().size());
        assertEquals(204L, ledger.newestFirst(SITE).getFirst().getAt());
        assertEquals(5L, ledger.newestFirst(SITE).getLast().getAt());
    }

    @Test
    void otherSitesAreIgnored() {
        ledger.onWages(new WageFundPaidEvent(SiteKey.of("elsewhere", CLAN), player, 10));

        assertTrue(camp.getLedger().isEmpty());
    }

    private static LedgerEntry entry(long at) {
        return new LedgerEntry(at, UUID.randomUUID(), "Alex", LedgerEntry.Kind.WAGES, List.of("1"));
    }
}
