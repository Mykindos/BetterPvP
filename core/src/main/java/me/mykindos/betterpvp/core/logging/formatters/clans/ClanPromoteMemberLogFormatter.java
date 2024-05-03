package me.mykindos.betterpvp.core.logging.formatters.clans;

import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;

import java.util.HashMap;

@WithReflection
public class ClanPromoteMemberLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "CLAN_PROMOTE";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return UtilMessage.deserialize("<yellow>%s</yellow> promoted <yellow>%s</yellow> from <green>%s</green> to <green>%s</green>",
                context.get(LogContext.CLIENT_NAME), context.get(LogContext.TARGET_CLIENT_NAME),
                context.get(LogContext.CURRENT_CLAN_RANK), context.get(LogContext.NEW_CLAN_RANK));
    }
}
