package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;

public interface WorldLogFormatter {

    String requiredAction();

    Component getPrefix();

    Component format(WorldLog log);
}
