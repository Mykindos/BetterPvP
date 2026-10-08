package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampWageFundTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final CampStore store = mock(CampStore.class);
    private final CampWageFund fund = new CampWageFund(store);

    @Test
    void ac41_theFundIsABalanceOnTheCampRecord() {
        final Camp camp = new Camp();
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        assertEquals(0, fund.balance(SITE));

        fund.deposit(SITE, 500);
        assertEquals(500, camp.getWageFund());
        assertEquals(500, fund.balance(SITE));

        fund.withdraw(SITE, 300);
        assertEquals(200, fund.balance(SITE));
        fund.withdraw(SITE, 1_000);
        assertEquals(0, fund.balance(SITE), "never below 0");
        verify(store, times(3)).changed(CLAN);
    }

    @Test
    void ac41_withNoCampLoadedNothingChanges() {
        when(store.cached(CLAN)).thenReturn(Optional.empty());
        assertEquals(0, fund.balance(SITE));
        fund.deposit(SITE, 500);
        fund.withdraw(SITE, 100);
        verify(store, never()).changed(anyLong());
    }
}
