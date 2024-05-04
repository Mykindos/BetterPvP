package me.mykindos.betterpvp.core.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;

import java.util.HashMap;

@Data
public class CachedLog {

    private final String message;
    private final String action;
    private final long timestamp;
    private final HashMap<String, String> context;

    public Component getTimeComponent() {
        return UtilMessage.deserialize("<white>" + UtilTime.getTime((System.currentTimeMillis() - timestamp), 2) + " ago</white> ");
    }

}
