package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Text on a screen: either a template with {@code {binding}} parts, or a translation key whose arguments are templates.
 */
@Value
public class TextSpec {

    @Nullable String template;
    @Nullable String key;
    List<String> args;

    public static TextSpec of(String template) {
        return new TextSpec(template, null, List.of());
    }

    public static TextSpec key(String key, String... args) {
        return new TextSpec(null, key, List.of(args));
    }
}
