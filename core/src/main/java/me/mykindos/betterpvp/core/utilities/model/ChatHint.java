package me.mykindos.betterpvp.core.utilities.model;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.jetbrains.annotations.NotNull;

/**
 * An icon at the end of a chat message that says there is more to it. {@link #INFO} explains the message,
 * {@link #MORE} holds the rest of something the message cut short, and {@link #EXPAND} opens a choice or a fuller view
 * when clicked.
 */
public enum ChatHint {

    INFO("\uE0A4"),
    MORE("\uE301"),
    EXPAND("\uE269");

    private final String glyph;

    ChatHint(@NotNull String glyph) {
        this.glyph = glyph;
    }

    /** {@code message} with this icon after it, showing {@code hover} when either is hovered. */
    public @NotNull Component attach(@NotNull Component message, @NotNull Component hover) {
        final HoverEvent<Component> event = HoverEvent.showText(hover);
        return Component.empty()
                .append(message.hoverEvent(event))
                .append(Component.space())
                .append(icon().hoverEvent(event));
    }

    /** As {@link #attach(Component, Component)}, running {@code click} when either is clicked. */
    public @NotNull Component attach(@NotNull Component message, @NotNull Component hover, @NotNull ClickEvent click) {
        return attach(message.clickEvent(click), hover).clickEvent(click);
    }

    private @NotNull Component icon() {
        return Component.text(glyph, NamedTextColor.WHITE)
                .font(Key.key("betterpvp", "input/chat"))
                .shadowColor(ShadowColor.none());
    }
}
