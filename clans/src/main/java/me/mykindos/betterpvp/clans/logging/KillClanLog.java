package me.mykindos.betterpvp.clans.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@Data
public class KillClanLog {

    private final String killerName;
    private final UUID killer;
    private final String killerClanName;
    private final UUID killerClan;

    private final String victimName;
    private final UUID victim;
    private final String victimClanName;
    private final UUID victimClan;

    private final double dominance;
    private final long time;

    public Component getTimeComponent() {
        return UtilMessage.deserialize("<white>" + UtilTime.getTime((System.currentTimeMillis() - this.time), 2) + " ago</white> ");
    }

}
