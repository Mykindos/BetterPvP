package me.mykindos.betterpvp.core.logging.formatters.items;

import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;

@WithReflection
public class ItemRetrieveLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "ITEM_RETRIEVE";
    }


    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW)
                .append(Component.text(" retrieved ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(context.get(LogContext.ITEM)))))
                .append(Component.text(" from ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.BLOCK), NamedTextColor.GREEN))
                .append(Component.text(" at ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.LOCATION), NamedTextColor.YELLOW));

    }
}
