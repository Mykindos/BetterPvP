package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * An absolute layout in GUI pixels. Elements are placed by their top-left corner, and later elements draw over
 * earlier ones on the same 9 px text line.
 * <p>
 * Content without a font uses {@link Resources.Font#UI}. Text is measured in its own font. Art glyphs come from the
 * {@code betterpvp:ui} font and need their size passed in, since art can be wider than the advance tables hold. Art
 * glyphs are drawn with their top at the line top (ascent 7) and are at least 8 px tall.
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
        return add(new CanvasElement(x, y, UtilFont.componentWidth(styled), DialogCompiler.LINE_HEIGHT, styled));
    }

    /** Places content with a known size. */
    public CanvasElement place(int x, int y, Component content, int width, int height) {
        return add(new CanvasElement(x, y, width, height, withDefaultFont(content)));
    }

    /** Places one art glyph from the {@code betterpvp:ui} font. */
    public CanvasElement art(int x, int y, char glyph, int width, int height) {
        return place(x, y, Component.text(glyph).font(Key.key("betterpvp", "ui")), width, height);
    }

    private CanvasElement add(CanvasElement element) {
        elements.add(element);
        return element;
    }

    private static Component withDefaultFont(Component content) {
        return content.font() == null ? content.font(Resources.Font.UI) : content;
    }
}
