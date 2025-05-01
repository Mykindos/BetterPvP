package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;

@Singleton
@BPvPListener
public class Died10TimesAchievement extends DeathAchievement {
    @Inject
    public Died10TimesAchievement() {
        super("Die 10 Times", 10);
    }
}
