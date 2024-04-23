package me.mykindos.betterpvp.core.logging.type.formatted;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;

/**
 * Represents a log from the database formatted nicely
 */
public abstract class FormattedLog {
    long time;
    protected FormattedLog(long time) {
        this.time = time;
    }

    public Component getTimeComponent() {
        return UtilMessage.deserialize("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green> ");
    }

    public Component getComponent() {
        return getTimeComponent();
    }
}
