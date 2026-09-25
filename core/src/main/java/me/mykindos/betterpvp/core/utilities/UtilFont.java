package me.mykindos.betterpvp.core.utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Pixel-advance calculators for the Minecraft default font. "Advance" is the cursor
 * movement a glyph causes: its visible width plus the 1px spacing the client inserts
 * after every glyph. Self-cancelling action bars and HUDs rely on them: a
 * block of text must be paid back with an equal negative {@code space.-N} so the
 * centered component's total advance (and therefore its on-screen position) is untouched.
 * <p>
 * Advances come from {@code font/default_advances.bin}, generated from the vanilla client's
 * font sheets and unifont by {@code core/tools/font_advances.py}.
 */
public final class UtilFont {

    private static final int UNIFONT_FLAG = 0x80;
    private static final byte[] ADVANCES = loadAdvances();

    private UtilFont() {
    }

    /** Pixel advance of a single default-font glyph (glyph width + 1px spacing). */
    public static int charWidth(char c) {
        return ADVANCES[c] & ~UNIFONT_FLAG;
    }

    /** Pixel advance of a string in the default font (sum of each glyph's advance). */
    public static int textWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += charWidth(text.charAt(i));
        }
        return width;
    }

    /**
     * Pixel advance of a fully-rendered component tree, rounded up the way the client rounds it.
     * Bold widens a sheet glyph by 1px and a unifont glyph by half a pixel, and {@code space.N}
     * nodes in the space font advance by N. Text in any other font is measured as the default
     * font. Resolve translatable nodes (e.g. via {@code Translations.render}) before calling,
     * otherwise their text isn't present to measure.
     */
    public static int componentWidth(Component component) {
        return (componentHalfWidth(component, false) + 1) / 2;
    }

    // Measured in half pixels so bold unifont glyphs stay exact until the final round-up.
    private static int componentHalfWidth(Component component, boolean parentBold) {
        final boolean bold = switch (component.decoration(TextDecoration.BOLD)) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> parentBold;
        };

        int width = 0;
        if (component instanceof TextComponent text) {
            width += textHalfWidth(text.content(), bold);
        } else if (component instanceof TranslatableComponent translatable
                && Resources.Font.SPACE.equals(component.font())
                && translatable.key().startsWith("space.")) {
            width += spaceAdvance(translatable.key()) * 2;
        }
        for (Component child : component.children()) {
            width += componentHalfWidth(child, bold);
        }
        return width;
    }

    private static int textHalfWidth(String text, boolean bold) {
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            final int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint > Character.MAX_VALUE) {
                width += (6 + (bold ? 1 : 0)) * 2; // missing-glyph box
                continue;
            }
            final int entry = ADVANCES[codePoint];
            width += (entry & ~UNIFONT_FLAG) * 2;
            if (bold) {
                width += (entry & UNIFONT_FLAG) != 0 ? 1 : 2;
            }
        }
        return width;
    }

    private static int spaceAdvance(String key) {
        try {
            return Integer.parseInt(key.substring("space.".length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static byte[] loadAdvances() {
        try (InputStream stream = UtilFont.class.getResourceAsStream("/font/default_advances.bin")) {
            if (stream == null) {
                throw new IllegalStateException("Missing font/default_advances.bin");
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
