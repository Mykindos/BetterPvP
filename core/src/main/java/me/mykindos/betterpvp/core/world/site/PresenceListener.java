package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps the tab list showing only the people a player can perceive.
 * <p>
 * Entities need no such handling: a player in another world is never sent to the client in the first place. The tab
 * list is the exception, being the one thing the server populates from the whole server rather than from what is
 * around you, so it is the only place separate instances leak into each other.
 */
@BPvPListener
@Singleton
public class PresenceListener implements Listener {

    private final Presence presence;
    private final EffectManager effects;

    @Inject
    public PresenceListener(@NotNull Presence presence, @NotNull EffectManager effects) {
        this.presence = presence;
        this.effects = effects;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    /** Rebuilds both directions between {@code player} and everybody else, since they have moved relative to all of them. */
    private void refresh(@NotNull Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }

            apply(player, other);
            apply(other, player);
        }
    }

    /**
     * Lists or unlists {@code subject} for {@code viewer}.
     * <p>
     * A vanished player is left unlisted whatever the presence rule says, because vanish unlists them the same way and
     * re-listing here would undo it the next time either of them changed world.
     */
    private void apply(@NotNull Player viewer, @NotNull Player subject) {
        if (presence.sees(viewer, subject) && !effects.hasEffect(subject, EffectTypes.VANISH)) {
            viewer.listPlayer(subject);
        } else {
            viewer.unlistPlayer(subject);
        }
    }
}
