package me.mykindos.betterpvp.core.locale;

import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link Translations#render(Component, String)} resolves server-side translation keys
 * into text (so item metadata never ships raw keys to the client).
 */
class TranslationsRenderTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @BeforeAll
    static void registerBundle() {
        GlobalTranslator.translator().addSource(TranslationService.translator());
        // Bundle resource lives in core's main resources (translations/core_en.properties), on the test classpath.
        TranslationService.translator().registerBundle(TranslationsRenderTest.class.getClassLoader(), "translations.core");
    }

    @Test
    @DisplayName("render resolves an item name key to its English text")
    void rendersItemName() {
        final Component rendered = Translations.render(Translations.component("core.item.stone.name"), (String) null);
        final String text = PLAIN.serialize(rendered);
        assertEquals("Stone", text);
        assertFalse(text.contains("core.item."), "rendered name still contains the raw key: " + text);
    }

    @Test
    @DisplayName("render resolves multi-line lore keys to English text")
    void rendersLore() {
        final Component[] lines = Translations.componentLines("core.item.smelter.lore");
        assertFalse(lines.length == 0, "expected lore lines for core.item.smelter.lore");
        for (Component line : lines) {
            final String text = PLAIN.serialize(Translations.render(line, (String) null));
            assertFalse(text.contains("core.item."), "rendered lore line still contains the raw key: " + text);
        }
        // First line of the smelter lore (see core_en.properties)
        assertEquals("Place and right-click this to", PLAIN.serialize(Translations.render(lines[0], (String) null)));
    }

    @Test
    @DisplayName("null / blank / invalid language resolves to the English locale")
    void toLocaleFallsBackToEnglish() {
        assertEquals(Locale.ENGLISH, Translations.toLocale(null));
        assertEquals(Locale.ENGLISH, Translations.toLocale(""));
        assertEquals(Locale.ENGLISH, Translations.toLocale("   "));
        assertEquals(Locale.ENGLISH, Translations.toLocale("@@@")); // not a parseable language tag
    }

    @Test
    @DisplayName("toLocale accepts es, es-ES and es_es (language = es; never uses country for lookup)")
    void toLocaleAcceptsLanguageTagForms() {
        assertEquals("es", Translations.toLocale("es").getLanguage());
        assertEquals("es", Translations.toLocale("es-ES").getLanguage());
        assertEquals("es", Translations.toLocale("es_es").getLanguage());
        // BCP-47 form preserves country, underscore form is normalized the same way
        assertEquals(Locale.forLanguageTag("es-ES"), Translations.toLocale("es-ES"));
        assertEquals(Locale.forLanguageTag("es-ES"), Translations.toLocale("es_es"));
    }

    @Test
    @DisplayName("null/invalid language renders English; es / es-ES / es_es all render Spanish")
    void rendersPerLocale() {
        final Component component = Translations.component("core.item.stone.name");
        // English / fallback paths
        assertEquals("Stone", PLAIN.serialize(Translations.render(component, (String) null)));
        assertEquals("Stone", PLAIN.serialize(Translations.render(component, "")));
        assertEquals("Stone", PLAIN.serialize(Translations.render(component, "en")));
        // All Spanish tag forms resolve to the Spanish bundle (core_es.properties: Piedra)
        for (String tag : new String[]{"es", "es-ES", "es_es"}) {
            final String es = PLAIN.serialize(Translations.render(component, tag));
            assertFalse(es.contains("core.item."), "rendered '" + tag + "' name still contains the raw key: " + es);
            assertEquals("Piedra", es, "expected Spanish for tag " + tag);
        }
    }

    @Test
    @DisplayName("menu button key renders Spanish (guards against es menu keys regressing)")
    void rendersMenuButtonSpanish() {
        final Component component = Translations.component("core.menu.button.next-page.name");
        assertEquals("Next Page", PLAIN.serialize(Translations.render(component, "en")));
        assertEquals("Página Siguiente", PLAIN.serialize(Translations.render(component, "es")));
    }

    @Test
    @DisplayName("missing locale-specific key falls back to English, never the raw key")
    void missingKeyFallsBackToEnglish() {
        // hi (Hindi) has no bundle shipped -> must fall back to English, not show the key
        final Component component = Translations.component("core.item.stone.name");
        final String hi = PLAIN.serialize(Translations.render(component, "hi"));
        assertEquals("Stone", hi);
    }

    @Test
    @DisplayName("wrap marker survives translation: a long translatable description wraps into multiple lines")
    void wrapMarkerSurvivesTranslation() {
        // tree-feller description is a long English string (see core_en.properties). When a translatable
        // component is flagged for wrapping, the marker must survive GlobalTranslator rendering so it can be
        // word-wrapped per-viewer at the packet/render boundary.
        final Component marked = ComponentWrapper.markForWrap(
                Translations.component("core.ability.tree-feller.description"), 30);
        final Component rendered = Translations.render(marked, "en");

        final List<Component> wrapped = ComponentWrapper.wrapIfMarked(rendered);
        assertTrue(wrapped.size() > 1, "expected the long description to wrap into multiple lines");

        final StringBuilder joined = new StringBuilder();
        for (Component line : wrapped) {
            assertNull(line.style().font(), "wrapped line still carries the wrap marker font");
            joined.append(PLAIN.serialize(line));
        }
        assertFalse(joined.toString().contains("core.ability."), "wrapped lore still contains the raw key");
        assertTrue(joined.toString().startsWith("Fells the entire tree"),
                "unexpected wrapped text: " + joined);
    }

    @Test
    @DisplayName("unmarked component is returned untouched by wrapIfMarked (no spurious wrapping)")
    void unmarkedComponentIsNotWrapped() {
        final Component rendered = Translations.render(Translations.component("core.item.stone.name"), "en");
        final List<Component> result = ComponentWrapper.wrapIfMarked(rendered);
        assertEquals(1, result.size());
        assertSame(rendered, result.get(0));
    }

    /**
     * Mirrors the shape {@code Skill#getValueComponent} produces for a level-dependent skill value: a
     * coloured value followed by the gain the next level would bring, e.g. {@code 8.0 (-1.0)}. Skill
     * descriptions pass this in as a MessageFormat argument ({@code Cooldown: {2}}), so the argument's
     * children must survive translation substitution.
     */
    private static Component skillValueWithNextLevelDelta() {
        return Component.text("8.0", NamedTextColor.GREEN)
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text("-1.0", NamedTextColor.GREEN))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    /** Flattens a rendered component into (text, effective colour) pairs, applying style inheritance. */
    private static void flatten(Component component, @Nullable TextColor inherited, List<String> out) {
        final TextColor colour = component.style().color() == null ? inherited : component.style().color();
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(text.content() + "@" + (colour == null ? "none" : colour.toString()));
        }
        for (Component child : component.children()) {
            flatten(child, colour, out);
        }
    }

    @Test
    @DisplayName("a multi-child skill value argument keeps its text through substitution in every locale")
    void skillValueArgumentSurvivesTranslation() {
        // core.command.balance.view is "Balance: {0}" / "Saldo: {0}" - the same shape as a skill's
        // "Cooldown: {2}" line, so it exercises the argument substitution skill descriptions rely on.
        final Component line = Translations.component("core.command.balance.view", skillValueWithNextLevelDelta());

        assertEquals("Balance: 8.0 (-1.0)", PLAIN.serialize(Translations.render(line, "en")));
        assertEquals("Saldo: 8.0 (-1.0)", PLAIN.serialize(Translations.render(line, "es")));
        // A locale with no bundle shipped must still fall back to English with the delta intact.
        assertEquals("Balance: 8.0 (-1.0)", PLAIN.serialize(Translations.render(line, "hi")));
    }

    @Test
    @DisplayName("the next-level delta keeps its green value / grey bracket colouring after translation")
    void skillValueArgumentKeepsColoursThroughTranslation() {
        final Component line = Translations.component("core.command.balance.view", skillValueWithNextLevelDelta());

        for (String tag : new String[]{"en", "es"}) {
            final List<String> parts = new ArrayList<>();
            flatten(Translations.render(line, tag), null, parts);
            assertTrue(parts.contains("8.0@" + NamedTextColor.GREEN), "value lost its green in " + tag + ": " + parts);
            assertTrue(parts.contains(" (@" + NamedTextColor.GRAY), "opening bracket lost its grey in " + tag + ": " + parts);
            assertTrue(parts.contains("-1.0@" + NamedTextColor.GREEN), "delta lost its green in " + tag + ": " + parts);
            assertTrue(parts.contains(")@" + NamedTextColor.GRAY), "closing bracket lost its grey in " + tag + ": " + parts);
        }
    }

    @Test
    @DisplayName("a wrapped description line keeps the delta attached to its value")
    void skillValueArgumentSurvivesWrapping() {
        // Lore is word-wrapped per-viewer after rendering; the delta must not be dropped by that pass.
        final Component marked = ComponentWrapper.markForWrap(
                Translations.component("core.command.balance.view", skillValueWithNextLevelDelta()), 10);
        final List<Component> wrapped = ComponentWrapper.wrapIfMarked(Translations.render(marked, "en"));

        final StringBuilder joined = new StringBuilder();
        for (Component wrappedLine : wrapped) {
            joined.append(PLAIN.serialize(wrappedLine));
        }
        assertTrue(joined.toString().replace("\n", "").contains("(-1.0)"),
                "wrapped line lost the next-level delta: " + joined);
    }

    @Test
    @DisplayName("render is cached: same component+locale returns the identical cached instance")
    void rendersAreCached() {
        final Component component = Translations.component("core.item.stone.name");
        final Component first = Translations.render(component, Locale.ENGLISH);
        final Component second = Translations.render(component, Locale.ENGLISH);
        assertSame(first, second, "expected cached render to return the same instance");
    }
}
