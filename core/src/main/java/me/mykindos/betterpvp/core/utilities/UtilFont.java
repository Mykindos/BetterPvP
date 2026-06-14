package me.mykindos.betterpvp.core.utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Pixel-advance calculators for the Minecraft default font. "Advance" is the cursor
 * movement a glyph causes: its visible width plus the 1px spacing the client inserts
 * after every glyph. These are used to build self-cancelling action bars/HUDs, where a
 * block of text must be paid back with an equal negative {@code space.-N} so the
 * centered component's total advance (and therefore its on-screen position) is untouched.
 */
public final class UtilFont {

    private UtilFont() {
    }

    /** Pixel advance of a single default-font glyph (glyph width + 1px spacing). */
    public static int charWidth(char c) {
        return switch (c) {
            case 'i', '!', '.', ',', ':', ';', '\'', '|' -> 2;
            case 'l', '`' -> 3;
            case 'I', 't', ' ', '[', ']', '▊' -> 4;
            case 'f', 'k', '"', '(', ')', '*', '<', '>', '{', '}' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }

    /** Pixel advance of a string in the default font (sum of each glyph's advance). */
    public static int textWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }
        return width;
    }

    /**
     * Pixel advance of a fully-rendered component tree in the default font, accounting for
     * inherited bold (which widens every glyph by 1px). Resolve any translatable nodes
     * (e.g. via {@code Translations.render}) before calling, otherwise their text isn't
     * present to measure.
     */
    public static int componentWidth(Component component) {
        return componentWidth(component, false);
    }

    private static int componentWidth(Component component, boolean parentBold) {
        final boolean bold = switch (component.decoration(TextDecoration.BOLD)) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> parentBold;
        };

        int width = 0;
        if (component instanceof TextComponent text) {
            for (char c : text.content().toCharArray()) {
                width += charWidth(c) + (bold ? 1 : 0);
            }
        }
        for (Component child : component.children()) {
            width += componentWidth(child, bold);
        }
        return width;
    }
}
