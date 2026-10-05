package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * An absolute layout in GUI pixels. Elements are placed by their top-left corner. Each 9 px text line draws its
 * elements left to right, and lower lines draw over higher ones.
 * <p>
 * Content without a font uses {@link Resources.Font#UI}. Text is measured in its own font. Art glyphs come from the
 * {@code betterpvp:ui} font and need their image size passed in, since art can be wider than the advance tables hold.
 * Art glyphs are drawn with their top at the line top (ascent 7) and are at least 8 px tall.
 * <p>
 * An element below a line top is drawn in a shifted copy of its font, so its fonts must have
 * {@link VerticalOffsets}. Characters the RPG font draws through the vanilla fallback (Arabic, Chinese, Japanese,
 * Korean) cannot be shifted, so text holding them snaps to the nearest line top instead.
 */
@Getter
public class DialogCanvas {

    private final int width;
    private final List<CanvasElement> elements = new ArrayList<>();

    public DialogCanvas(int width) {
        this.width = width;
    }

    /**
     * Places text, measured in its font, one text line tall. Render translatable text for the viewer first
     * ({@code Translations.render}), since unresolved text measures as zero.
     */
    public CanvasElement text(int x, int y, Component content) {
        final Component styled = withDefaultFont(content);
        final int line = DialogCompiler.LINE_HEIGHT;
        final int placedY = hasFallbackCharacters(styled) ? Math.round((float) y / line) * line : y;
        return place(x, placedY, styled, UtilFont.componentWidth(styled), line);
    }

    /** Places content with a known advance width and height. */
    public CanvasElement place(int x, int y, Component content, int width, int height) {
        final Component styled = withDefaultFont(content);
        // Advances end with the 1 px gap after the last drawn pixel, which may overhang the edge.
        if (x < 0 || y < 0 || x + width > this.width + 1) {
            throw new IllegalArgumentException("Element at (" + x + ", " + y + ") " + width + " px wide does not fit a canvas "
                    + this.width + " px wide: " + styled);
        }
        checkShiftable(styled, y % DialogCompiler.LINE_HEIGHT, Resources.Font.UI);
        final CanvasElement element = new CanvasElement(x, y, width, height, styled);
        elements.add(element);
        return element;
    }

    /** Places one art glyph from the {@code betterpvp:ui} font, {@code width} by {@code height} px. */
    public CanvasElement art(int x, int y, char glyph, int width, int height) {
        // The client advances a bitmap glyph one pixel past its image.
        return place(x, y, Component.text(glyph).font(Key.key("betterpvp", "ui")), width + 1, height);
    }

    private static Component withDefaultFont(Component content) {
        return content.font() == null ? content.font(Resources.Font.UI) : content;
    }

    private static void checkShiftable(Component component, int offset, Key inherited) {
        if (offset == 0) {
            return;
        }
        final Key font = component.font() != null ? component.font() : inherited;
        if (!font.equals(Resources.Font.SPACE)) {
            VerticalOffsets.font(font, offset);
        }
        component.children().forEach(child -> checkShiftable(child, offset, font));
    }

    /** Scripts the RPG font leaves to vanilla: Hebrew to Arabic and beyond, CJK, Hangul and compatibility forms. */
    private static boolean isFallback(int point) {
        return (point >= 0x0590 && point < 0x2000) || (point >= 0x2E80 && point < 0xE000) || point >= 0xF900;
    }

    private static boolean hasFallbackCharacters(Component component) {
        if (component instanceof TextComponent text && text.content().codePoints().anyMatch(DialogCanvas::isFallback)) {
            return true;
        }
        return component.children().stream().anyMatch(DialogCanvas::hasFallbackCharacters);
    }
}
