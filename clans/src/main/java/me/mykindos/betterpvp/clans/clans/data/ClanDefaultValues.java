package me.mykindos.betterpvp.clans.clans.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.Config;

@Getter
@Singleton
public class ClanDefaultValues {

    @Inject
    @Config(path = "clans.clan.default.energy", defaultValue = "2400")
    private int defaultEnergy;

    @Inject
    @Config(path = "clans.clan.default.level", defaultValue = "1")
    private int defaultLevel;

    @Inject
    @Config(path = "clans.clan.default.points", defaultValue = "0")
    private int defaultPoints;

}
