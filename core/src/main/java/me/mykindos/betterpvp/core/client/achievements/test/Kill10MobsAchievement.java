package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;

@Singleton
@BPvPListener
public class Kill10MobsAchievement extends MobKillsAchievement {
    @Inject
    public Kill10MobsAchievement() {
        super("Kill 10 Mobs", 10);
    }
}
