package me.mykindos.betterpvp.core.utilities.model;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.object.ObjectContents;
import org.jetbrains.annotations.NotNull;

/**
 * The icon that leads a chat line without a prefix. {@link #NEWS} marks something that happened, {@link #PROBLEM}
 * something that needs the player, and {@link #TIP} a hint.
 */
public enum ChatIcon {

    NEWS("bell"),
    PROBLEM("exclamation_mark"),
    TIP("info");

    private final String sprite;

    ChatIcon(@NotNull String sprite) {
        this.sprite = sprite;
    }

    /** {@code message} led by this icon. */
    public @NotNull Component line(@NotNull Component message) {
        final Component icon = Component.object(ObjectContents.sprite(Key.key("gui"),
                Key.key("betterpvp", "icon/chat/" + sprite)));
        return Component.join(JoinConfiguration.spaces(), icon, message);
    }
}
