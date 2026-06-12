package me.mykindos.betterpvp.core.item.component.impl.socketables.runes;

import net.kyori.adventure.text.Component;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

/**
 * Helper for building the styled value arguments substituted into translatable rune lore lines.
 *
 * <p>Rune descriptions historically embedded custom MiniMessage tags (e.g. {@code <damage>5.0</damage>})
 * directly in their text. Since the lore is now translatable (the surrounding sentence lives in the
 * translation bundle as plain {@code {0}}-placeholder text, with NO MiniMessage tags), the dynamic,
 * locale-independent values are pre-rendered into {@link Component} arguments here. Each value is
 * deserialized through the exact same custom tag, guaranteeing identical styling (colour + font icon
 * glyph) to the original inline form while keeping the translated text tag-free.</p>
 */
public final class RuneLore {

    private RuneLore() {
    }

    private static Component tag(String tag, String value) {
        return miniMessage.deserialize("<" + tag + ">" + value + "</" + tag + ">");
    }

    public static Component damage(String value) {
        return tag("damage", value);
    }

    public static Component health(String value) {
        return tag("health", value);
    }

    public static Component mana(String value) {
        return tag("mana", value);
    }

    public static Component coins(String value) {
        return tag("coins", value);
    }

    public static Component exp(String value) {
        return tag("exp", value);
    }

    public static Component time(String value) {
        return tag("time", value);
    }

    public static Component val(String value) {
        return tag("val", value);
    }
}
