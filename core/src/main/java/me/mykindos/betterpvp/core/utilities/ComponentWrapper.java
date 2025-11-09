package me.mykindos.betterpvp.core.utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_RED;

public class ComponentWrapper {

    private static final TextComponent UNSUPPORTED = text("ERROR WRAPPING").color(DARK_RED);
    private static final int DEFAULT_LINE_LENGTH = 30;

    public static List<Component> wrapLine(final Component component) {
        return wrapLine(component, DEFAULT_LINE_LENGTH);
    }

    public static List<Component> wrapLine(final Component component, final int length) {
        return wrapLine(component, length, false);
    }

    public static List<Component> wrapLine(final Component component, final int length, final boolean allowLastWordOverflow) {
        if (!(component instanceof final TextComponent text)) {
            return Collections.singletonList(component);
        }

        final List<Component> wrapped = new ArrayList<>();
        final List<TextComponent> parts = flattenText(text);

        Component currentLine = empty();
        int lineLength = 0;

        for (final TextComponent part : parts) {

            final Style style = part.style();
            final String content = part.content();
            final String[] words = content.split("(?<=\\s)|(?=\\n)");

            for (final String word : words) {

                if (word.isEmpty()) {
                    continue;
                }

                final int wordLength = word.length();
                final int totalLength = lineLength + wordLength;
                
                if (word.contains("\n")) {
                    wrapped.add(currentLine);
                    currentLine = empty().style(style);
                    lineLength = 0;
                } else if (totalLength > length) {
                    // Check if this is the last word and we're allowing overflow
                    boolean isLastWord = false;
                    if (allowLastWordOverflow) {
                        // Check if this is potentially the last word on the line
                        int nextPartIndex = parts.indexOf(part) + 1;
                        boolean isLastPart = nextPartIndex >= parts.size();
                        boolean isLastWordInPart = Arrays.asList(words).indexOf(word) == words.length - 1;
                        
                        isLastWord = (isLastPart && isLastWordInPart) || word.endsWith("\n");
                    }
                    
                    if (!isLastWord || lineLength == 0) {
                        wrapped.add(currentLine);
                        currentLine = empty().style(style);
                        lineLength = 0;
                    }
                }

                if (!word.equals("\n")) {
                    currentLine = currentLine.append(text(word).style(style));
                    lineLength += wordLength;
                }
            }
        }

        if (lineLength > 0) {
            wrapped.add(currentLine);
        }

        return wrapped;
    }

    private static List<TextComponent> flattenText(final TextComponent component) {
        final List<TextComponent> flattened = new ArrayList<>();
        final Style style = component.style();
        final Style enforcedState = enforceStyleStates(style);
        final TextComponent first = component.style(enforcedState);

        final Stack<TextComponent> toCheck = new Stack<>();
        toCheck.add(first);

        while (!toCheck.empty()) {

            final TextComponent parent = toCheck.pop();
            final String content = parent.content();
            if (!content.isEmpty()) {
                flattened.add(parent);
            }

            final List<Component> children = parent.children();
            final List<Component> reversed = children.reversed();
            for (final Component child : reversed) {
                if (child instanceof final TextComponent text) {
                    final Style parentStyle = parent.style();
                    final Style textStyle = text.style();
                    final Style merge = parentStyle.merge(textStyle);
                    final TextComponent childComponent = text.style(merge);
                    toCheck.add(childComponent);
                } else {
                    toCheck.add(UNSUPPORTED);
                }
            }
        }
        return flattened;
    }

    private static Style enforceStyleStates(final Style style) {
        final Style.Builder builder = style.toBuilder();
        final Map<TextDecoration, TextDecoration.State> map = style.decorations();
        map.forEach((decoration, state) -> {
            if (state == TextDecoration.State.NOT_SET) {
                builder.decoration(decoration, false);
            }
        });
        return builder.build();
    }

}
