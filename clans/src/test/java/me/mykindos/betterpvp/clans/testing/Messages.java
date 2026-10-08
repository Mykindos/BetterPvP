package me.mykindos.betterpvp.clans.testing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.bukkit.entity.Player;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

/** Reads the chat messages sent to a mocked player and the text inside components. */
public final class Messages {

    private Messages() {
    }

    /** Every message sent to {@code player}, oldest first, or none. */
    public static List<Component> sent(Player player) {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        try {
            verify(player, atLeast(1)).sendMessage(captor.capture());
        } catch (AssertionError none) {
            return List.of();
        }
        return captor.getAllValues();
    }

    /** Whether any message sent to {@code player} mentions translation key {@code key}. */
    public static boolean told(Player player, String key) {
        return sent(player).stream().anyMatch(message -> mentions(message, key));
    }

    /** Whether {@code component}, its translation arguments or its children use translation key {@code key}. */
    public static boolean mentions(Component component, String key) {
        if (component instanceof TranslatableComponent translatable) {
            if (translatable.key().equals(key)) {
                return true;
            }
            for (TranslationArgument argument : translatable.arguments()) {
                if (mentions(argument.asComponent(), key)) {
                    return true;
                }
            }
        }
        return component.children().stream().anyMatch(child -> mentions(child, key));
    }

    /** The literal text of {@code component} and everything in it, translation keys in brackets. */
    public static String text(Component component) {
        final StringBuilder builder = new StringBuilder();
        collect(component, builder);
        return builder.toString();
    }

    private static void collect(Component component, StringBuilder builder) {
        if (component instanceof TextComponent text) {
            builder.append(text.content());
        }
        if (component instanceof TranslatableComponent translatable) {
            builder.append('[').append(translatable.key()).append(']');
            translatable.arguments().forEach(argument -> collect(argument.asComponent(), builder));
        }
        component.children().forEach(child -> collect(child, builder));
    }
}
