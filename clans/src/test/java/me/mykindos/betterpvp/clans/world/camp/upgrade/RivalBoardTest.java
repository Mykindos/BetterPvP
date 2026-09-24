package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.ProsperityStanding;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivalBoardTest {

    @Test
    void keepsTheCampsRankedJustAboveAndBelow() {
        final Map<Long, ProsperityStanding> standings = new HashMap<>();
        for (long clan = 1; clan <= 20; clan++) {
            standings.put(clan, new ProsperityStanding((int) clan * 100, (int) clan * 100));
        }

        final List<RivalBoard.Row> rows = RivalBoard.rank(standings, 10, 1000, 2, 3);

        assertEquals(List.of(12L, 11L, 10L, 9L, 8L, 7L), rows.stream().map(RivalBoard.Row::getClanId).toList());
        assertEquals(List.of(9, 10, 11, 12, 13, 14), rows.stream().map(RivalBoard.Row::getRank).toList());
        assertTrue(rows.get(2).isOwn());
        assertEquals(1, rows.stream().filter(RivalBoard.Row::isOwn).count());
    }

    @Test
    void theTopCampHasNothingAbove() {
        final Map<Long, ProsperityStanding> standings = Map.of(
                1L, new ProsperityStanding(500, 500),
                2L, new ProsperityStanding(300, 300),
                3L, new ProsperityStanding(100, 100));

        final List<RivalBoard.Row> rows = RivalBoard.rank(standings, 1, 500, 5, 5);

        assertEquals(List.of(1L, 2L, 3L), rows.stream().map(RivalBoard.Row::getClanId).toList());
    }

    @Test
    void changeIsMeasuredAgainstTheSnapshotAndTheOwnCampIsLive() {
        final Map<Long, ProsperityStanding> standings = Map.of(
                1L, new ProsperityStanding(400, 450),
                2L, new ProsperityStanding(300, 200));

        final List<RivalBoard.Row> rows = RivalBoard.rank(standings, 2, 700, 5, 5);

        assertEquals(2L, rows.getFirst().getClanId(), "the live value ranks the own camp");
        assertEquals(700, rows.getFirst().getProsperity());
        assertEquals(500, rows.getFirst().getChange());
        assertEquals(-50, rows.get(1).getChange());
    }

    @Test
    void anUnrecordedCampStillAppearsWithNoChange() {
        final List<RivalBoard.Row> rows = RivalBoard.rank(Map.of(1L, new ProsperityStanding(100, 90)), 7, 50, 5, 5);

        assertEquals(List.of(1L, 7L), rows.stream().map(RivalBoard.Row::getClanId).toList());
        assertEquals(0, rows.get(1).getChange());
        assertEquals(2, rows.get(1).getRank());
    }

    @Test
    void tiesAreBrokenByClanId() {
        final Map<Long, ProsperityStanding> standings = Map.of(
                3L, new ProsperityStanding(100, 100),
                1L, new ProsperityStanding(100, 100));

        final List<RivalBoard.Row> rows = RivalBoard.rank(standings, 2, 100, 5, 5);

        assertEquals(List.of(1L, 2L, 3L), rows.stream().map(RivalBoard.Row::getClanId).toList());
    }
}
