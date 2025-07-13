package me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.DeathAchievement;
import me.mykindos.betterpvp.core.listener.BPvPListener;

@Singleton
/**
 * In code instantiation. This allows the option of overriding the description
 */
@BPvPListener
public class Died10TimesAchievement extends DeathAchievement {
    @Inject
    public Died10TimesAchievement() {
        super("death_10", 10);
    }
}
