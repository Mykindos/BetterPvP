package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

/**
 * Fluent builder for pixel-precise action-bar / HUD layouts. Minecraft centers an action bar
 * on its total horizontal advance, so these layouts move a virtual cursor with the "space" font
 * ({@link #space}) to position glyphs, then pay every shift back so the net advance — and
 * therefore the on-screen position — stays fixed.
 * <p>
 * It wraps the repetitive {@code Component.translatable("space.N").font(SPACE)} and
 * {@code Component.text(...).font(...)} idiom so call sites read as drawing commands rather than
 * component plumbing. A single signed {@link #space(int)} replaces the error-prone split between
 * {@code "space.-" + n} and {@code "space." + n} string prefixes.
 */
public class FontCanvas {

    /** Namespace for this project's bitmap fonts (hud, conversation, offset/*, input/*, ...). */
    private static final String NAMESPACE = "betterpvp";

    private final TextComponent.Builder builder = Component.text();

    /** Resolve a {@code betterpvp:}-namespaced bitmap font key. */
    public static Key font(String font) {
        return Key.key(NAMESPACE, font);
    }

    /** Shift the cursor by {@code pixels} (negative moves left) via the space font. No-op for 0. */
    public FontCanvas space(int pixels) {
        if (pixels != 0) {
            builder.append(Component.translatable("space." + pixels).font(Resources.Font.SPACE));
        }
        return this;
    }

    public FontCanvas glyph(char glyph, TextColor color, Key font) {
        builder.append(Component.text(glyph, color).font(font));
        return this;
    }

    public FontCanvas glyph(char glyph, TextColor color, String font) {
        return glyph(glyph, color, font(font));
    }

    /** Draw an uncoloured glyph (inherits the surrounding colour). */
    public FontCanvas glyph(char glyph, String font) {
        builder.append(Component.text(glyph).font(font(font)));
        return this;
    }

    public FontCanvas text(String text, TextColor color, Key font) {
        builder.append(Component.text(text, color).font(font));
        return this;
    }

    public FontCanvas text(String text, TextColor color, String font) {
        return text(text, color, font(font));
    }

    /** Draw uncoloured text (inherits the surrounding colour). */
    public FontCanvas text(String text, String font) {
        builder.append(Component.text(text).font(font(font)));
        return this;
    }

    /** Append an already-built component verbatim (e.g. a queued action-bar message). */
    public FontCanvas append(Component component) {
        builder.append(component);
        return this;
    }

    public Component build() {
        return builder.build();
    }

    private static final int ICON_GAP = 4;

    public enum Alignment { CENTER, RIGHT }

    public enum IconSide { LEFT, RIGHT }

    /**
     * Draw a labelled locator bar — a tinted pill with a centred label and an icon hung off one side —
     * as a net-zero block, so vanilla centres its origin and the shader maps that origin to a screen
     * edge (see {@link HudAnchor}). Pass {@link HudAnchor#color()} as {@code barColor} to re-anchor it;
     * the {@code start}/{@code mid}/{@code end} glyphs let each readout pick its own pill design.
     *
     * @param label the pre-built, pre-coloured label in its {@code offset/down_N} font (resolve translatables first)
     * @param row   the {@code hud/down_N} row for the bar and icon
     * @param inset gap in pixels from the anchored edge (ignored for {@link Alignment#CENTER})
     */
    public FontCanvas labeledBar(Component label, TextColor barColor, int row,
                                 char start, char mid, char end,
                                 char icon, int iconWidth, IconSide iconSide,
                                 Alignment alignment, int inset) {
        final int width = UtilFont.componentWidth(label);
        final int middles = (width + 1) / 2 + 10 * 2;
        final int boxWidth = (middles + 2) * 2;
        final int iconAdvance = iconWidth + 1;

        final int barCenterX;
        final int iconLeftX;
        if (alignment == Alignment.RIGHT) {
            if (iconSide == IconSide.RIGHT) {
                iconLeftX = -inset - iconWidth;
                barCenterX = iconLeftX - ICON_GAP - boxWidth / 2;
            } else {
                barCenterX = -inset - boxWidth / 2;
                iconLeftX = barCenterX - boxWidth / 2 - ICON_GAP - iconWidth;
            }
        } else {
            barCenterX = 0;
            iconLeftX = iconSide == IconSide.LEFT
                    ? -(boxWidth / 2 + ICON_GAP + iconWidth)
                    : boxWidth / 2 + ICON_GAP;
        }

        final Key rowFont = font("hud/down_" + row);
        space(barCenterX);
        pill(boxWidth, middles, rowFont, barColor, start, mid, end);
        space(-width / 2).append(label).space(-(width - width / 2));
        space(-barCenterX);

        space(iconLeftX).glyph(icon, barColor, rowFont).space(-(iconLeftX + iconAdvance));
        return this;
    }

    private void pill(int boxWidth, int middles, Key rowFont, TextColor color, char start, char mid, char end) {
        space(-boxWidth / 2);
        space(-1).glyph(start, color, rowFont);
        for (int i = 0; i < middles; i++) {
            space(-1).glyph(mid, color, rowFont);
        }
        space(-1).glyph(end, color, rowFont);
        space(-(boxWidth - boxWidth / 2));
    }
}
