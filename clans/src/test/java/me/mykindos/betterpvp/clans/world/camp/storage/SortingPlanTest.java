package me.mykindos.betterpvp.clans.world.camp.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingPlanTest {

    private static List<SortingPlan.Move> plan(List<List<SortingPlan.Pile<String>>> chests,
                                               List<SortingPlan.Pile<String>> items) {
        return SortingPlan.plan(chests, items, Objects::equals, kind -> kind.equals("pearl") ? 16 : 64);
    }

    private static SortingPlan.Pile<String> pile(String kind, int amount) {
        return new SortingPlan.Pile<>(kind, amount);
    }

    private static final SortingPlan.Pile<String> EMPTY = null;

    @SafeVarargs
    private static List<SortingPlan.Pile<String>> chest(SortingPlan.Pile<String>... slots) {
        return new ArrayList<>(Arrays.asList(slots));
    }

    @Test
    void topsUpMatchingStacksBeforeUsingEmptySlots() {
        final List<SortingPlan.Move> moves = plan(
                List.of(chest(EMPTY, EMPTY), chest(pile("stone", 60), EMPTY)),
                List.of(pile("stone", 10)));

        assertEquals(List.of(new SortingPlan.Move(0, 1, 0, 4), new SortingPlan.Move(0, 1, 1, 6)), moves);
    }

    @Test
    void prefersEmptySlotsInChestsAlreadyHoldingTheItem() {
        final List<SortingPlan.Move> moves = plan(
                List.of(chest(EMPTY), chest(pile("stone", 64), EMPTY)),
                List.of(pile("stone", 5)));

        assertEquals(List.of(new SortingPlan.Move(0, 1, 1, 5)), moves);
    }

    @Test
    void fallsBackToEmptySlotsInAnyChest() {
        final List<SortingPlan.Move> moves = plan(
                List.of(chest(pile("dirt", 1), EMPTY), chest(EMPTY)),
                List.of(pile("stone", 5)));

        assertEquals(List.of(new SortingPlan.Move(0, 0, 1, 5)), moves);
    }

    @Test
    void respectsTheStackLimitOfTheKind() {
        final List<SortingPlan.Move> moves = plan(List.of(chest(EMPTY, EMPTY)), List.of(pile("pearl", 20)));

        assertEquals(List.of(new SortingPlan.Move(0, 0, 0, 16), new SortingPlan.Move(0, 0, 1, 4)), moves);
    }

    @Test
    void leavesOutWhatFindsNoRoom() {
        final List<SortingPlan.Move> moves = plan(List.of(chest(pile("dirt", 64))), List.of(pile("stone", 5)));

        assertTrue(moves.isEmpty());
    }

    @Test
    void laterItemsSeeWhatEarlierItemsPlaced() {
        final List<SortingPlan.Move> moves = plan(List.of(chest(EMPTY, EMPTY)),
                List.of(pile("stone", 40), pile("stone", 40)));

        assertEquals(List.of(new SortingPlan.Move(0, 0, 0, 40), new SortingPlan.Move(1, 0, 0, 24),
                new SortingPlan.Move(1, 0, 1, 16)), moves);
    }
}
