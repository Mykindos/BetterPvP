package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Rejects logins from UUIDs banned in code, independent of the punishments table.
 */
@BPvPListener
@Singleton
@CustomLog
public class HardcodedBanListener implements Listener {

    private static final Set<UUID> BANNED = Set.of(
            UUID.fromString("2626d120-3b83-43b3-826b-d58267f22ffc")
    );

    // HIGHEST so no earlier handler can allow the login back through
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!BANNED.contains(event.getUniqueId())) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                Translations.render(Translations.component("core.punishment.hardcoded_ban"), (Locale) null));
        log.info("Rejected login from hardcoded banned UUID {} ({})", event.getUniqueId(), event.getName()).submit();
    }

}
