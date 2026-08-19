package me.mykindos.betterpvp.core.loot;

import me.mykindos.betterpvp.core.loot.expression.ExpressionEngine;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Covers the bonus roll phase of {@link LootTable#generateLoot(LootContext)}. Bonus rolls are additive on
 * top of the table's own roll count and always draw with replacement, so a bonus roll is worth the same on
 * a narrow table as on a wide one.
 */
class LootTableBonusRollsTest {

    private LootSession session;
    private Location location;

    /**
     * Minimal concrete {@link Loot} so tables can be built without touching the item stack pipeline.
     */
    private static final class NamedLoot extends Loot<String, String> {

        private final String name;

        private NamedLoot(String name, ReplacementStrategy strategy) {
            super(strategy, context -> true);
            this.name = name;
        }

        @Override
        public String getReward() {
            return name;
        }

        @Override
        protected String award(LootContext context) {
            return name;
        }

        @Override
        public ItemView getIcon() {
            return ItemView.builder().build();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @BeforeEach
    void setUp() {
        final World world = Mockito.mock(World.class);
        location = new Location(world, 0, 0, 0);
        session = Mockito.mock(LootSession.class);
        when(session.getProgress()).thenReturn(new LootProgress());
    }

    private LootContext context(Map<String, Object> inputs) {
        return new LootContext(session, location, LootSource.of("Test", "test"), inputs);
    }

    private LootTable table(ReplacementStrategy strategy, int rolls, Loot<?, ?>... entries) {
        final List<WeightedEntry> weighted = Arrays.stream(entries)
                .map(loot -> WeightedEntry.of(loot, 10))
                .toList();
        return LootTable.builder()
                .id("test")
                .replacementStrategy(strategy)
                .rollCountFunction(RollCountFunction.constant(rolls))
                .weightedEntries(new ArrayList<>(weighted))
                .build();
    }

    @Test
    void bonusRollsAddToTheBaseRollCount() {
        final LootTable table = table(ReplacementStrategy.WITH_REPLACEMENT, 2,
                new NamedLoot("a", ReplacementStrategy.UNSET),
                new NamedLoot("b", ReplacementStrategy.UNSET));

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 3)));

        assertEquals(5, bundle.getLoot().size());
    }

    @Test
    void noBonusRollsInputLeavesTableUnchanged() {
        final LootTable table = table(ReplacementStrategy.WITH_REPLACEMENT, 2,
                new NamedLoot("a", ReplacementStrategy.UNSET));

        assertEquals(2, table.generateLoot(context(Map.of())).getLoot().size());
    }

    @Test
    void negativeBonusRollsAreClampedToZero() {
        final LootTable table = table(ReplacementStrategy.WITH_REPLACEMENT, 2,
                new NamedLoot("a", ReplacementStrategy.UNSET));

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, -5)));

        assertEquals(2, bundle.getLoot().size());
    }

    @Test
    void bonusRollsDrawWithReplacementOnAWithoutReplacementTable() {
        // Two entries, one base roll, ten bonus rolls. Without the fresh bonus pool the table would
        // exhaust itself after two draws.
        final LootTable table = table(ReplacementStrategy.WITHOUT_REPLACEMENT, 1,
                new NamedLoot("a", ReplacementStrategy.UNSET),
                new NamedLoot("b", ReplacementStrategy.UNSET));

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 10)));

        assertEquals(11, bundle.getLoot().size());
    }

    @Test
    void basePhaseStillHonoursWithoutReplacement() {
        final LootTable table = table(ReplacementStrategy.WITHOUT_REPLACEMENT, 5,
                new NamedLoot("a", ReplacementStrategy.UNSET),
                new NamedLoot("b", ReplacementStrategy.UNSET));

        // Pool of two, so five base rolls can only produce two entries.
        assertEquals(2, table.generateLoot(context(Map.of())).getLoot().size());
    }

    @Test
    void entryLevelWithoutReplacementStaysOncePerBundleAcrossBothPhases() {
        final NamedLoot unique = new NamedLoot("unique", ReplacementStrategy.WITHOUT_REPLACEMENT);
        final LootTable table = table(ReplacementStrategy.WITH_REPLACEMENT, 1, unique);

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 8)));

        final long uniqueCount = bundle.getLoot().stream().filter(loot -> loot == unique).count();
        assertEquals(1, uniqueCount,
                "an entry that opts into WITHOUT_REPLACEMENT must not be duplicated by bonus rolls");
    }

    @Test
    void bonusRollsApplyEvenWhenTheBaseRollCountIsZero() {
        final LootTable table = table(ReplacementStrategy.WITH_REPLACEMENT, 0,
                new NamedLoot("a", ReplacementStrategy.UNSET));

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 4)));

        assertEquals(4, bundle.getLoot().size());
    }

    @Test
    void rollIndexKeepsIncrementingAcrossBothPhases() {
        // A weight function that only pays out from roll index 2 onwards proves the bonus phase continues
        // the index rather than restarting it.
        final NamedLoot late = new NamedLoot("late", ReplacementStrategy.UNSET);
        final LootTable table = LootTable.builder()
                .id("test")
                .rollCountFunction(RollCountFunction.constant(2))
                .weightedEntries(new ArrayList<>(List.of(
                        WeightedEntry.of(late, ctx -> {
                            final Object index = ctx.getInput(ExpressionEngine.VAR_ROLL_INDEX);
                            return index instanceof Number number && number.intValue() >= 2 ? 10 : 0;
                        }, 10))))
                .build();

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 2)));

        assertEquals(2, bundle.getLoot().size(), "only the two bonus rolls should have had non-zero weight");
    }

    @Test
    void bonusRollsAreVisibleToRollCountExpressions() {
        final LootTable table = LootTable.builder()
                .id("test")
                .rollCountFunction(RollCountFunction.expression("bonus_rolls > 0 ? 2 : 1", 1))
                .weightedEntries(new ArrayList<>(List.of(
                        WeightedEntry.of(new NamedLoot("a", ReplacementStrategy.UNSET), 10))))
                .build();

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 1)));

        // 2 base rolls, because the expression saw bonus_rolls, plus the 1 bonus roll itself
        assertEquals(3, bundle.getLoot().size());
    }

    @Test
    void guaranteedLootIsAwardedOnceRegardlessOfBonusRolls() {
        final NamedLoot guaranteed = new NamedLoot("guaranteed", ReplacementStrategy.UNSET);
        final LootTable table = LootTable.builder()
                .id("test")
                .rollCountFunction(RollCountFunction.constant(1))
                .guaranteedLoot(new ArrayList<>(List.of(guaranteed)))
                .weightedEntries(new ArrayList<>(List.of(
                        WeightedEntry.of(new NamedLoot("a", ReplacementStrategy.UNSET), 10))))
                .build();

        final LootBundle bundle = table.generateLoot(context(Map.of(ExpressionEngine.VAR_BONUS_ROLLS, 3)));

        assertEquals(1, bundle.getLoot().stream().filter(loot -> loot == guaranteed).count());
        assertEquals(5, bundle.getLoot().size());
        assertTrue(bundle.getLoot().contains(guaranteed));
    }
}
