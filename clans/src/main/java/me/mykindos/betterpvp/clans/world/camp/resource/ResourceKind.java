package me.mykindos.betterpvp.clans.world.camp.resource;

import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/** The resources a camp builds with. */
public enum ResourceKind {
    WOOD(TextColor.color(0xA9, 0x74, 0x4F)),
    STONE(TextColor.color(0x9E, 0x9E, 0x9E)),
    IRON(TextColor.color(0xDC, 0xDC, 0xDC));

    private final TextColor color;

    ResourceKind(@NotNull TextColor color) {
        this.color = color;
    }

    /** The id construction costs are written under. */
    public @NotNull String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @NotNull Component displayName() {
        return Translations.component("clans.camp.resource." + id());
    }

    /** The RGB color this resource is shown in, matching its material. */
    public @NotNull TextColor color() {
        return color;
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
