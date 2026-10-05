package me.mykindos.betterpvp.core.menu.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

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

    private DialogCompiler() {
    }

    /** The body text. {@code clicks} gives the click key of the element at each index. */
    static Component body(DialogCanvas canvas, IntFunction<Key> clicks) {
        throw new UnsupportedOperationException();
    }

    /** Number of text lines the body needs so every element fits inside it. */
    static int lines(DialogCanvas canvas) {
        throw new UnsupportedOperationException();
    }

    /** The title text that draws the backdrop over a body canvas {@code canvasWidth} wide. */
    static Component backdrop(DialogCanvas backdrop, int canvasWidth) {
        throw new UnsupportedOperationException();
    }
}
