package me.mykindos.betterpvp.core.menu.dialog;

import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VerticalOffsets")
class VerticalOffsetsTest {

    @Test
    @DisplayName("offsets in range resolve to the generated fonts")
    void inRange() {
        assertEquals(Resources.Font.UI, VerticalOffsets.font(Resources.Font.UI, 0));
        assertEquals(Key.key("betterpvp", "rpg/down_3"), VerticalOffsets.font(Resources.Font.UI, 3));
        assertEquals(Key.key("betterpvp", "ui/down_8"), VerticalOffsets.font(Key.key("betterpvp", "ui"), 8));
    }

    @Test
    @DisplayName("AC10: an offset outside the generated range fails naming the offset")
    void ac10_outOfRangeFailsNamingOffset() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> VerticalOffsets.font(Resources.Font.UI, 9));
        assertTrue(error.getMessage().contains("9"));
    }
}
