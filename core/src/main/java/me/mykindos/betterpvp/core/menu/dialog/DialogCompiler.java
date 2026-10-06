package me.mykindos.betterpvp.core.menu.dialog;

import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Turns canvases into dialog text. The body canvas becomes one {@code plain_message}: each element sits on the
 * 9 px line that holds its top, is shifted down the rest with a {@link VerticalOffsets} font, and is reached across
 * the line with negative space. The backdrop becomes the title, with a net advance of zero so the client draws it
 * from the screen centre.
 */
final class DialogCompiler {

    /** Text inset of a {@code plain_message} on each side. */
    static final int TEXT_INSET = 4;
    static final int LINE_HEIGHT = 9;

    /**
     * Colour of hover art. Its low three bits per channel (5, 3, 5) are the signature the pack's text shader looks
     * for, kept in step with {@code rendertype_text.vsh}.
     */
    static final TextColor HOVER_SIGNATURE = TextColor.color(0xFDFBFD);

    /** The client draws a zero-width title from this many pixels left of the screen centre. */
    private static final int TITLE_ORIGIN = 15;

    private DialogCompiler() {
    }

    /** The body text. {@code clicks} gives the click key of the element at each index, or null. */
    static Component body(DialogCanvas canvas, IntFunction<Key> clicks) {
        return body(canvas, clicks, null, null);
    }

    /**
     * The body text. Every line is padded to the canvas width, since the client centres each line. Clicks anywhere
     * that no element handles go to {@code background}, so the server can re-render and clear the focus outline the
     * client draws around a clicked text block.
     */
    static Component body(DialogCanvas canvas, IntFunction<Key> clicks, Key background, CanvasElement pressed) {
        final List<CanvasElement> elements = canvas.getElements();
        final int lines = lines(canvas);
        final TextComponent.Builder body = Component.text();
        if (background != null) {
            body.clickEvent(ClickEvent.custom(background, BinaryTagHolder.binaryTagHolder("{}")));
        }
        for (int line = 0; line < lines; line++) {
            if (line > 0) {
                body.append(Component.newline());
            }
            final List<Integer> onLine = new ArrayList<>();
            for (int index = 0; index < elements.size(); index++) {
                if (elements.get(index).getY() / LINE_HEIGHT == line) {
                    onLine.add(index);
                }
            }
            onLine.sort(Comparator.comparingInt(index -> elements.get(index).getX()));

            final List<Integer> reaching = reaching(elements, clicks, line);
            // The pressed art shows without hover art, which would otherwise keep drawing over it.
            final Component pressedHover = pressed == null ? null : pressed.getHover();
            int cursor = 0;
            for (int index : onLine) {
                final CanvasElement element = elements.get(index);
                appendGap(body, cursor, element.getX(), elements, reaching, clicks);
                final Component content = element == pressed ? element.getPressed() : element.getContent();
                body.append(interactive(shift(content, element.getY() % LINE_HEIGHT), element, clicks.apply(index),
                        pressedHover != null && element.getHover() == pressedHover));
                cursor = element.getX() + element.getWidth();
            }
            appendGap(body, cursor, canvas.getWidth(), elements, reaching, clicks);
        }
        return body.build();
    }

    /** Clickable elements that start on an earlier line and reach down into {@code line}. */
    private static List<Integer> reaching(List<CanvasElement> elements, IntFunction<Key> clicks, int line) {
        final List<Integer> reaching = new ArrayList<>();
        for (int index = 0; index < elements.size(); index++) {
            final CanvasElement element = elements.get(index);
            if (element.getY() / LINE_HEIGHT < line && element.getY() + element.getHeight() > line * LINE_HEIGHT
                    && clicks.apply(index) != null) {
                reaching.add(index);
            }
        }
        return reaching;
    }

    /**
     * Space from {@code from} to {@code to}. The client resolves a click to the last clickable text under the mouse,
     * line by line, so blank space on a line would take the click from the lower part of an element above. Where an
     * element from an earlier line reaches into the gap, the space carries that element's click.
     */
    private static void appendGap(TextComponent.Builder body, int from, int to, List<CanvasElement> elements,
                                  List<Integer> reaching, IntFunction<Key> clicks) {
        int cursor = from;
        while (cursor < to) {
            Integer cover = null;
            int next = to;
            for (int index : reaching) {
                final CanvasElement element = elements.get(index);
                final int right = element.getX() + element.getWidth();
                if (element.getX() <= cursor && right > cursor) {
                    cover = index;
                    next = Math.min(right, to);
                    break;
                }
                if (element.getX() > cursor) {
                    next = Math.min(next, element.getX());
                }
            }
            if (cover == null) {
                appendSpace(body, next - cursor);
            } else {
                body.append(Component.translatable("space." + (next - cursor)).font(Resources.Font.SPACE)
                        .clickEvent(ClickEvent.custom(clicks.apply(cover), BinaryTagHolder.binaryTagHolder("{}"))));
            }
            cursor = next;
        }
        if (cursor > to) {
            appendSpace(body, to - cursor);
        }
    }

    /** Number of text lines the body needs so every element, and the canvas height it was made with, fits inside it. */
    static int lines(DialogCanvas canvas) {
        int bottom = Math.max(LINE_HEIGHT, canvas.getHeight());
        for (CanvasElement element : canvas.getElements()) {
            bottom = Math.max(bottom, element.getY() + element.getHeight());
        }
        return (bottom + LINE_HEIGHT - 1) / LINE_HEIGHT;
    }

    /**
     * The title text that draws the backdrop over a body canvas {@code canvasWidth} wide. Backdrop art carries its
     * vertical position in its glyph ascent, and an element's y shifts it down by up to {@link VerticalOffsets#MAX}.
     */
    static Component backdrop(DialogCanvas backdrop, int canvasWidth) {
        final List<CanvasElement> elements = new ArrayList<>(backdrop.getElements());
        elements.sort(Comparator.comparingInt(CanvasElement::getX));

        final TextComponent.Builder title = Component.text().shadowColor(ShadowColor.none());
        final int origin = TITLE_ORIGIN - canvasWidth / 2;
        int cursor = 0;
        for (CanvasElement element : elements) {
            if (element.getY() > VerticalOffsets.MAX) {
                throw new IllegalArgumentException("Backdrop element at y " + element.getY() + " is too low. Backdrop art carries "
                        + "its height in its glyph ascent, and y only shifts it 0 to " + VerticalOffsets.MAX + " px");
            }
            final int target = origin + element.getX();
            appendSpace(title, target - cursor);
            title.append(shift(element.getContent(), element.getY()));
            cursor = target + element.getWidth();
        }
        appendSpace(title, -cursor);
        return title.build();
    }

    private static void appendSpace(TextComponent.Builder builder, int pixels) {
        if (pixels != 0) {
            builder.append(Component.translatable("space." + pixels).font(Resources.Font.SPACE));
        }
    }

    private static Component interactive(Component content, CanvasElement element, Key click, boolean pressed) {
        Component result = content;
        if (click != null) {
            result = result.clickEvent(ClickEvent.custom(click, BinaryTagHolder.binaryTagHolder("{}")));
        }
        if (pressed) {
            return result;
        }
        if (element.getHover() != null) {
            result = result.hoverEvent(hoverArt(element.getHover()));
        } else if (element.getTooltip() != null) {
            result = result.hoverEvent(HoverEvent.showText(element.getTooltip()));
        }
        return result;
    }

    /**
     * An item tooltip whose name is the hover art, framed by the invisible {@code betterpvp:hover} tooltip style. The
     * art carries {@link #HOVER_SIGNATURE} so the pack's text shader moves it from the mouse onto its element.
     */
    private static HoverEvent<HoverEvent.ShowItem> hoverArt(Component art) {
        final Component name = art.color(HOVER_SIGNATURE).shadowColor(ShadowColor.none())
                .decoration(TextDecoration.ITALIC, false);
        return HoverEvent.showItem(HoverEvent.ShowItem.showItem(Key.key("paper"), 1, Map.of(
                Key.key("custom_name"), BinaryTagHolder.binaryTagHolder(GsonComponentSerializer.gson().serialize(name)),
                Key.key("tooltip_style"), BinaryTagHolder.binaryTagHolder("\"betterpvp:hover\""))));
    }

    /** Moves every font in the tree {@code offset} pixels down. Negative space stays as it is. */
    private static Component shift(Component component, int offset) {
        return shift(component, offset, Resources.Font.UI);
    }

    private static Component shift(Component component, int offset, Key inherited) {
        if (offset == 0) {
            return component;
        }
        final Key font = component.font() != null ? component.font() : inherited;
        final Component shifted = font.equals(Resources.Font.SPACE) ? component : component.font(VerticalOffsets.font(font, offset));
        return shifted.children(shifted.children().stream().map(child -> shift(child, offset, font)).toList());
    }
}
