package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.description.Description;

@Singleton
@BPvPListener
public class Died10TimesAchievement extends DeathAchievement {
    @Inject
    public Died10TimesAchievement() {
        super("death_10", 10);
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(Gamer container) {
        return null;
    }
}
