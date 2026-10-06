package me.mykindos.betterpvp.core.menu.dialog.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Expressions")
class ExpressionsTest {

    private final GuiRegistry registry = new GuiRegistry();
    private final Map<String, Object> state = Map.of(
            "tab", "skills",
            "coins", 1500.0,
            "cost", 300.0,
            "skill", Map.of("name", "Dash", "tags", List.of("move", "fast")));

    private Object eval(String template) {
        return Expressions.evaluate(template, state::get, registry.formatters());
    }

    @Test
    @DisplayName("a lone binding keeps its type, mixed text becomes a string")
    void templates() {
        assertEquals(1500.0, eval("{coins}"));
        assertEquals("Coins: 1500 of 300", eval("Coins: {coins} of {cost}"));
        assertEquals("Dash", eval("{skill.name}"));
        assertEquals("fast", eval("{skill.tags[1]}"));
    }

    @Test
    @DisplayName("comparisons and logic")
    void logic() {
        assertEquals(true, eval("{tab == 'skills'}"));
        assertEquals(false, eval("{tab != 'skills'}"));
        assertEquals(true, eval("{coins >= cost && !(tab == 'builds')}"));
        assertEquals(true, eval("{missing || coins > 0}"));
    }

    @Test
    @DisplayName("arithmetic, and + joining text")
    void arithmetic() {
        assertEquals(1800.0, eval("{coins + cost}"));
        assertEquals(true, eval("{coins - cost * 5 == 0}"));
        assertEquals("skills!", eval("{tab + '!'}"));
    }

    @Test
    @DisplayName("formatters, with arguments")
    void formatters() {
        assertEquals("1.5k", eval("{coins | short}"));
        assertEquals("SKILLS", eval("{tab | upper}"));
        assertEquals("Nobody", eval("{missing | default:'Nobody'}"));
    }

    @Test
    @DisplayName("an unknown formatter names itself")
    void unknownFormatter() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> eval("{coins | nope}"));
        assertEquals(true, error.getMessage().contains("nope"));
    }
}
