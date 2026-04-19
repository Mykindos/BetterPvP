package me.mykindos.betterpvp.core.client.achievements.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class AchievementListener implements Listener {

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

    @UpdateEvent(delay = 1000L * 60 * 10, isAsync = true)
    public void updateTotalRanks () {
        achievementManager.updateTotalAchievementCompletions();
    }

    /**
     * Periodically scan all loaded clients against all registered achievements.
     * Catches completions missed by the event-driven path (new achievements, edge cases, etc.).
     */
    @UpdateEvent(delay = 1000L * 60 * 5, isAsync = true)
    public void periodicAchievementCheck() {
        clientManager.getLoaded().forEach(client ->
                achievementManager.getObjects().values().forEach(achievement ->
                        achievement.forceCheck(client.getStatContainer())
                )
        );
    }
}
