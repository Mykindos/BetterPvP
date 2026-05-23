package me.mykindos.betterpvp.core.loot.expression;

import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootProgress;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ExpressionEngineTest {

    private LootContext context;

    @BeforeEach
    void setUp() {
        final World world = Mockito.mock(World.class);
        final Location location = new Location(world, 0, 0, 0);
        final LootSession session = Mockito.mock(LootSession.class);
        when(session.getProgress()).thenReturn(new LootProgress());
        context = new LootContext(session, location, LootSource.of("Test", "test"), Map.of(
                "slayer_standing", 1,
                "dungeon_score", 75,
                "tier", "boss",
                "is_first", true
        ));
    }

    @Test
    void evaluatesArithmetic() {
        assertEquals(3.0, ExpressionEngine.evalDouble("1 + 2", context, Map.of(), 0));
    }

    @Test
    void readsContextInputs() {
        assertEquals(3.0, ExpressionEngine.evalDouble("fn:clamp(4 - slayer_standing, 1, 3)", context, Map.of(), 0));
    }

    @Test
    void supportsStringComparisons() {
        assertTrue(ExpressionEngine.evalBoolean("tier == 'boss'", context, Map.of(), false));
        assertFalse(ExpressionEngine.evalBoolean("tier == 'trash'", context, Map.of(), true));
    }

    @Test
    void supportsBooleanInputs() {
        assertTrue(ExpressionEngine.evalBoolean("is_first && dungeon_score > 50", context, Map.of(), false));
    }

    @Test
    void missingVariableEvaluatesToZero() {
        // strict(false) treats missing vars as null; arithmetic on null returns 0
        assertEquals(0.0, ExpressionEngine.evalDouble("not_a_var * 2", context, Map.of(), 42));
    }

    @Test
    void unparseableExpressionReturnsFallback() {
        assertEquals(42.0, ExpressionEngine.evalDouble("@@@ syntax error", context, Map.of(), 42));
    }

    @Test
    void extrasOverrideInputs() {
        double v = ExpressionEngine.evalDouble("slayer_standing", context, Map.of("slayer_standing", 99), 0);
        assertEquals(99.0, v);
    }

    @Test
    void rejectsMethodCallsAndNew() {
        // method calls on classes are blocked by sandbox
        assertNull(ExpressionEngine.eval("''.getClass().getName()", context));
        assertNull(ExpressionEngine.eval("new('java.lang.String', 'x')", context));
    }

    @Test
    void rejectsOverlongExpression() {
        String huge = "1+".repeat(300) + "1";
        assertEquals(7.0, ExpressionEngine.evalDouble(huge, context, Map.of(), 7));
    }
}
