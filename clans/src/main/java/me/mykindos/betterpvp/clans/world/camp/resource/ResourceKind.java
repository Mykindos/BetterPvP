package me.mykindos.betterpvp.clans.world.camp.resource;

import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/** The resources a camp builds with. */
public enum ResourceKind {
    WOOD,
    STONE,
    IRON;

    /** The id construction costs are written under. */
    public @NotNull String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @NotNull Component displayName() {
        return Translations.component("clans.camp.resource." + id());
    }

    public static @NotNull Optional<ResourceKind> byId(@NotNull String id) {
        for (ResourceKind kind : values()) {
            if (kind.id().equalsIgnoreCase(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
