package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WagePolicyTest {

    private static final UUID ANNA = UUID.randomUUID();
    private static final UUID BRAM = UUID.randomUUID();
    private static final UUID CORA = UUID.randomUUID();

    @Test
    void theShortfallIsSplitEvenly() {
        final Map<UUID, Long> shares = WagePolicy.split(90, room(100, 100, 100));

        assertEquals(Map.of(ANNA, 30L, BRAM, 30L, CORA, 30L), shares);
    }

    @Test
    void whoeverCannotPayAFullShareLeavesTheRestToOthers() {
        final Map<UUID, Long> shares = WagePolicy.split(90, room(10, 100, 100));

        assertEquals(Map.of(ANNA, 10L, BRAM, 40L, CORA, 40L), shares);
    }

    @Test
    void anUnevenAmountIsNeverOverpaid() {
        final Map<UUID, Long> shares = WagePolicy.split(10, room(100, 100, 100));

        assertEquals(10, shares.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void withTooLittleRoomEveryonePaysTheirMost() {
        final Map<UUID, Long> shares = WagePolicy.split(500, room(10, 20, 30));

        assertEquals(Map.of(ANNA, 10L, BRAM, 20L, CORA, 30L), shares);
    }

    @Test
    void roomIsTheLesserOfBalanceAndWhatTheCapLeaves() {
        final Camp camp = new Camp();
        camp.setWageContributionDay(7);
        camp.getWageContributions().put(ANNA, 400L);
        camp.getWageContributions().put(BRAM, 500L);

        final Map<UUID, Long> room = WagePolicy.room(camp, Map.of(ANNA, 1_000L, BRAM, 1_000L, CORA, 50L), 500, 7);

        assertEquals(Map.of(ANNA, 100L, CORA, 50L), room);
    }

    @Test
    void aNewDayStartsTheCapAgain() {
        final Camp camp = new Camp();
        camp.setWageContributionDay(7);
        camp.getWageContributions().put(ANNA, 500L);

        assertEquals(Map.of(ANNA, 500L), WagePolicy.room(camp, Map.of(ANNA, 1_000L), 500, 8));

        WagePolicy.startDay(camp, 8);
        assertTrue(camp.getWageContributions().isEmpty());
        assertEquals(8, camp.getWageContributionDay());
    }

    private static Map<UUID, Long> room(long anna, long bram, long cora) {
        final Map<UUID, Long> room = new LinkedHashMap<>();
        room.put(ANNA, anna);
        room.put(BRAM, bram);
        room.put(CORA, cora);
        return room;
    }
}
