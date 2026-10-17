package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;

import java.util.Arrays;

/**
 * Brigadier messages built from translation keys. Paper keeps the component inside the message, so each player
 * reads it in their own language.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandMessages {

    /**
     * @param key  the translation key
     * @param args the placeholders, as components or as values shown with {@link String#valueOf(Object)}
     * @return a message that renders the key in the reader's language
     */
    public static Message translatable(String key, Object... args) {
        final ComponentLike[] components = Arrays.stream(args)
                .map(arg -> arg instanceof ComponentLike like ? like : Component.text(String.valueOf(arg)))
                .toArray(ComponentLike[]::new);
        return MessageComponentSerializer.message().serialize(Translations.component(key, components));
    }

    /**
     * @param message a Brigadier message
     * @return the message as a component, with any translatable parts kept
     */
    public static Component component(Message message) {
        return MessageComponentSerializer.message().deserialize(message);
    }
}
