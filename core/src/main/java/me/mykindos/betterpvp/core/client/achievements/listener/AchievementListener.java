package me.mykindos.betterpvp.core.client.achievements.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class AchievementListener implements Listener {

    private final AchievementManager achievementManager;

    @Inject
    public AchievementListener(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    @UpdateEvent(delay = 1000L * 60 * 10, isAsync = true)
    public void updateTotalRanks () {
        achievementManager.updateTotalAchievementCompletions();
    }
}
