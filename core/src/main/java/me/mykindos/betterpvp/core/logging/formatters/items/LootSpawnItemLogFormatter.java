package me.mykindos.betterpvp.core.logging.formatters.items;

import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;

@WithReflection
public class LootSpawnItemLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "ITEM_LOOT_SPAWN";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN)
                .append(Component.text(" was looted from ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.SOURCE), NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(context.get(LogContext.ITEM)))))
                .append(Component.text(" at ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.LOCATION), NamedTextColor.YELLOW));

    }
}
