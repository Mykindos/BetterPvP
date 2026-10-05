package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Tracks the dialog screen each player has open and routes its clicks. Every screen sent gets a new id, so clicks
 * from a screen that was replaced or closed are ignored.
 */
@BPvPListener
@Singleton
public class DialogSessions implements Listener {

    private final DialogSender sender;

    @Inject
    public DialogSessions(DialogSender sender) {
        this.sender = sender;
    }

    /** Shows {@code screen}, replacing any screen the player has open. */
    public void open(Player player, DialogScreen screen) {
        throw new UnsupportedOperationException();
    }

    /** Sends the open screen again, with the last input values as initial values. */
    public void rerender(Player player) {
        throw new UnsupportedOperationException();
    }

    public void close(Player player) {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen(UUID player) {
        throw new UnsupportedOperationException();
    }

    /** Routes a custom click. Returns false when the key is not a click of the player's open screen. */
    public boolean handle(Player player, Key action, DialogInputs inputs) {
        throw new UnsupportedOperationException();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        throw new UnsupportedOperationException();
    }
}
