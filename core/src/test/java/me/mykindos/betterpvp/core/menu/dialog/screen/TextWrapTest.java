package me.mykindos.betterpvp.core.menu.dialog.screen;

import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Text wrapping")
class TextWrapTest {

    private static final Component TEXT = Component.text("Hits hard and fast with a sword in each hand").font(Resources.Font.UI);

    @Test
    @DisplayName("every wrapped line fits the width and no word is lost")
    void linesFit() {
        final List<Component> lines = TextWrap.lines(TEXT, 80, 0);
        assertTrue(lines.size() > 1);
        lines.forEach(line -> assertTrue(UtilFont.componentWidth(line) - 1 <= 80, line.toString()));
        assertEquals("Hits hard and fast with a sword in each hand",
                String.join(" ", lines.stream().map(line -> ((TextComponent) line).content()).toList()));
    }

    @Test
    @DisplayName("a line cap ends the last line with an ellipsis")
    void capped() {
        final List<Component> lines = TextWrap.lines(TEXT, 80, 2);
        assertEquals(2, lines.size());
        assertTrue(((TextComponent) lines.get(1)).content().endsWith("..."));
    }

    @Test
    @DisplayName("opening a screen keeps null state values")
    void openKeepsNulls() {
        final Map<String, Object> state = new HashMap<>();
        state.put("error", null);
        assertNull(ActionResult.open("next", state).getState().get("error"));
    }
}
