package me.mykindos.betterpvp.core.world.construction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/** Looks through a component's tree for translation keys and text, so tests do not depend on how it is put together. */
public final class ComponentKeys {

    private ComponentKeys() {
    }

    public static boolean hasKey(@NotNull Component component, @NotNull String key) {
        return find(component, key).isPresent();
    }

    /** The first translatable under {@code key} anywhere in {@code component}, its arguments included. */
    public static @NotNull Optional<TranslatableComponent> find(@NotNull Component component, @NotNull String key) {
        if (component instanceof TranslatableComponent translatable) {
            if (translatable.key().equals(key)) {
                return Optional.of(translatable);
            }
            for (TranslationArgument argument : translatable.arguments()) {
                final Optional<TranslatableComponent> found = find(argument.asComponent(), key);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        for (Component child : component.children()) {
            final Optional<TranslatableComponent> found = find(child, key);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Every literal text in {@code component}, in order, translatable arguments included. */
    public static @NotNull String text(@NotNull Component component) {
        final StringBuilder out = new StringBuilder();
        collect(component, out);
        return out.toString();
    }

    private static void collect(@NotNull Component component, @NotNull StringBuilder out) {
        if (component instanceof TextComponent text) {
            out.append(text.content());
        }
        if (component instanceof TranslatableComponent translatable) {
            for (TranslationArgument argument : translatable.arguments()) {
                collect(argument.asComponent(), out);
            }
        }
        component.children().forEach(child -> collect(child, out));
    }
}
