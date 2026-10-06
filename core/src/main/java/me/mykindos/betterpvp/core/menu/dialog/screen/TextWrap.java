package me.mykindos.betterpvp.core.menu.dialog.screen;

import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks styled text into lines that fit a width, between words. Each line takes the text's own style, so colours set
 * on parts of the text are not kept.
 */
public final class TextWrap {

    private static final String ELLIPSIS = "...";

    private TextWrap() {
    }

    /**
     * @param maxLines the most lines to return, the last one cut with an ellipsis when the text runs on; 0 for no limit
     */
    public static List<Component> lines(Component text, int width, int maxLines) {
        final List<Component> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        final String[] words = PlainTextComponentSerializer.plainText().serialize(text).split(" ");
        for (int index = 0; index < words.length; index++) {
            final String candidate = line.isEmpty() ? words[index] : line + " " + words[index];
            if (line.isEmpty() || fits(text, candidate, width)) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (maxLines > 0 && lines.size() == maxLines - 1) {
                lines.add(cut(text, line + " " + String.join(" ", List.of(words).subList(index, words.length)), width));
                return lines;
            }
            lines.add(styled(text, line.toString()));
            line = new StringBuilder(words[index]);
        }
        if (!line.isEmpty()) {
            lines.add(fits(text, line.toString(), width) ? styled(text, line.toString()) : cut(text, line.toString(), width));
        }
        return lines;
    }

    /** Pixels between wrapped lines: the menu line, or two for the heading size. */
    public static int lineHeight(Component text) {
        return Resources.Font.UI_LARGE.equals(text.font()) ? 18 : 9;
    }

    private static Component cut(Component text, String content, int width) {
        String kept = content;
        while (!kept.isEmpty() && !fits(text, kept + ELLIPSIS, width)) {
            kept = kept.substring(0, kept.length() - 1);
        }
        return styled(text, kept.stripTrailing() + ELLIPSIS);
    }

    private static boolean fits(Component text, String content, int width) {
        return UtilFont.componentWidth(styled(text, content)) - 1 <= width;
    }

    private static Component styled(Component text, String content) {
        return Component.text(content).style(text.style());
    }
}
