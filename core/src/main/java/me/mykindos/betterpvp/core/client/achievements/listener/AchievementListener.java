package me.mykindos.betterpvp.core.client.achievements.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.AchievementManager;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class AchievementListener implements Listener {

    private final AchievementManager achievementManager;

    @Inject
    public AchievementListener(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClientJoin(ClientJoinEvent event) {
        achievementManager.loadContainer(event.getClient());
        achievementManager.loadContainer(event.getClient().getGamer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClientQuit(ClientQuitEvent event) {
        achievementManager.unloadId(event.getClient().getUniqueId());
    }

    @UpdateEvent(delay = 1000L * 500, isAsync = true)
    public void updateTotalRanks () {
        achievementManager.updateTotalAchievementCompletions();
    }
}
