package me.mykindos.betterpvp.core.i18n;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

public record TranslationArgument(@NotNull String name, @NotNull TagResolver resolver) {

    public static @NotNull TranslationArgument component(@NotNull String name, @NotNull ComponentLike value) {
        return new TranslationArgument(name, Placeholder.component(name, value.asComponent()));
    }

    public static @NotNull TranslationArgument parsed(@NotNull String name, @NotNull String value) {
        return new TranslationArgument(name, Placeholder.parsed(name, value));
    }

    public static @NotNull TranslationArgument unparsed(@NotNull String name, @NotNull Object value) {
        return new TranslationArgument(name, Placeholder.unparsed(name, String.valueOf(value)));
    }
}
