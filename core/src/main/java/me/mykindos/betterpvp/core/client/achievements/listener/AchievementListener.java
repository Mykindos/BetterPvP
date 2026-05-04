package me.mykindos.betterpvp.core.client.achievements.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class AchievementListener implements Listener {

    @Config(path = "achievements.enabled", defaultValue = "true")
    @Inject
    private boolean achievementsEnabled;

    private final AchievementManager achievementManager;
    private final ClientManager clientManager;

    @Inject
    public AchievementListener(AchievementManager achievementManager, ClientManager clientManager) {
        this.achievementManager = achievementManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onClientLoadEvent(AsyncClientLoadEvent event) {
        achievementManager.loadAchievementCompletionsAsync(event.getClient());
    }

    /**
     * Centralised stat-change handler. Uses the stat→achievement index to dispatch only to
     * achievements that actually watch the changed stat, avoiding a full scan on every update.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatPropertyUpdate(final StatPropertyUpdateEvent event) {
        if (!achievementsEnabled) return;

        final Set<IAchievement> affected = achievementManager.getAchievementsForStat(event.getStat());
        if (affected.isEmpty()) return;

        for (IAchievement achievement : affected) {
            try {
                achievement.onPropertyChangeListener(event);
            } catch (Exception e) {
                log.error("Error dispatching stat update to achievement {}", achievement.getName(), e).submit();
            }
        }
    }

    @UpdateEvent(delay = 1000L * 60 * 10, isAsync = true)
    public void updateTotalRanks() {
        achievementManager.updateTotalAchievementCompletions();
    }

    /**
     * Periodically scan all online clients against all registered achievements.
     * Catches completions missed by the event-driven path (new achievements, edge cases, etc.).
     * Restricted to online clients to avoid unnecessary work and DB writes for offline players.
     */
    @UpdateEvent(delay = 1000L * 60 * 5, isAsync = true)
    public void periodicAchievementCheck() {
        if (!achievementsEnabled) return;
        clientManager.getOnline().forEach(client ->
                achievementManager.getObjects().values().forEach(achievement -> {
                    try {
                        achievement.forceCheck(client.getStatContainer());
                    } catch (Exception e) {
                        log.error("Error while force-checking achievement {} for client {}: ", achievement.getNamespacedKey(), client.getName(), e).submit();
                    }
                })
        );
    }
}
