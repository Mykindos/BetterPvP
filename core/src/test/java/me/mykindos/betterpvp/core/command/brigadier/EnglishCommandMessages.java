package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import me.mykindos.betterpvp.core.locale.TranslationService;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Stands in for Paper's {@link MessageComponentSerializer}, which only exists on a running server. A serialized
 * message keeps its component and reads as the English text from the core translation bundle.
 */
public final class EnglishCommandMessages implements AutoCloseable {

    private static boolean bundleRegistered;
    private final MockedStatic<MessageComponentSerializer> serializerStatic;

    /**
     * A message that remembers the component it was made from, as Paper's own does.
     */
    public static final class ComponentMessage extends LiteralMessage {
        private final Component component;

        ComponentMessage(Component component) {
            super(english(component));
            this.component = component;
        }

        public Component component() {
            return component;
        }
    }

    private EnglishCommandMessages() {
        registerBundle();
        final MessageComponentSerializer serializer = mock(MessageComponentSerializer.class);
        when(serializer.serialize(any(Component.class))).thenAnswer(invocation -> new ComponentMessage(invocation.getArgument(0)));
        when(serializer.deserialize(any(Message.class))).thenAnswer(invocation -> {
            final Message message = invocation.getArgument(0);
            return message instanceof ComponentMessage componentMessage ? componentMessage.component() : Component.text(message.getString());
        });
        serializerStatic = Mockito.mockStatic(MessageComponentSerializer.class);
        serializerStatic.when(MessageComponentSerializer::message).thenReturn(serializer);
    }

    public static EnglishCommandMessages open() {
        return new EnglishCommandMessages();
    }

    public static synchronized void registerBundle() {
        if (!bundleRegistered) {
            GlobalTranslator.translator().addSource(TranslationService.translator());
            TranslationService.translator().registerBundle(EnglishCommandMessages.class.getClassLoader(), "translations.core");
            bundleRegistered = true;
        }
    }

    /**
     * The component as an English player would read it.
     */
    public static String english(Component component) {
        registerBundle();
        return PlainTextComponentSerializer.plainText().serialize(Translations.render(component, (String) null));
    }

    @Override
    public void close() {
        serializerStatic.close();
    }
}
