package me.mykindos.betterpvp.core.quest.conversation;

import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Resolves a line of dialogue for one reader: a translation key when the node names one, the literal text when it does
 * not.
 * <p>
 * Dialogue is typed out a character at a time and laid out by measuring pixel widths, so the renderer needs a plain
 * {@link String} and not a {@link Component} - which is why a translated line has to be flattened here, at the point
 * where the reader's locale is known, rather than passed down as a translatable and left to the client.
 * <p>
 * A line may carry arguments, filling the {@code {0}}, {@code {1}} placeholders of its translation. They are supplied
 * as components and rendered with the line, so an argument that is itself a translation speaks the same language as
 * the sentence around it.
 */
public final class ConversationText {

    private ConversationText() {
    }

    /**
     * @param key     a translation key, or null/blank when the line is written literally
     * @param literal the fallback text, used whenever there is no key or no translation registered under it
     * @param locale  the reader's locale
     * @param args    values for the translation's placeholders, in order; empty when it has none
     * @return the line as it should be shown to that reader, never null
     */
    public static @NotNull String resolve(@Nullable String key, @Nullable String literal, @Nullable Locale locale,
                                          @Nullable List<Component> args) {
        if (key == null || key.isBlank() || !Translations.hasTranslation(key)) {
            // An unregistered key falls back rather than showing the key itself: a missing translation should read as
            // untranslated dialogue, not as a broken line of machine text in the middle of a conversation.
            return literal == null ? "" : literal;
        }
        final ComponentLike[] resolved = args == null ? new ComponentLike[0] : args.toArray(new ComponentLike[0]);
        final Component rendered = Translations.render(Translations.component(key, resolved), locale);
        return PlainTextComponentSerializer.plainText().serialize(rendered);
    }

    /** A line with no placeholders to fill. See {@link #resolve(String, String, Locale, List)}. */
    public static @NotNull String resolve(@Nullable String key, @Nullable String literal, @Nullable Locale locale) {
        return resolve(key, literal, locale, null);
    }
}
