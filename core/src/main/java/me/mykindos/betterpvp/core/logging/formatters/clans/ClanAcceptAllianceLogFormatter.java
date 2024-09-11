package me.mykindos.betterpvp.core.logging.formatters.clans;

import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;

@WithReflection
public class ClanAcceptAllianceLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "CLAN_ALLIANCE_ACCEPT";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return UtilMessage.deserialize("<yellow>%s</yellow> accepted alliance with clan <yellow>%s</yellow>",
                context.get(LogContext.CLIENT_NAME), context.get(LogContext.TARGET_CLAN_NAME));
    }

    @Override
    public Description getDescription(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        HashMap<String, String> context = cachedLog.getContext();
        List<Component> lore = List.of(
                UtilMessage.DIVIDER,
                cachedLog.getRelativeTimeComponent(),
                cachedLog.getAbsoluteTimeComponent(),
                UtilMessage.DIVIDER,
                Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW),
                Component.text("accepted alliance with"),
                Component.text(context.get(LogContext.TARGET_CLAN_NAME), NamedTextColor.GREEN),
                Component.text(context.get(LogContext.CLAN_NAME), NamedTextColor.AQUA),
                UtilMessage.DIVIDER
        );

        ItemProvider itemProvider = ItemView.builder()
                .displayName(UtilMessage.deserialize("<yellow>%s</yellow> <green>accept alliance</green> <green>%s</green>" ,
                        context.get(LogContext.CLIENT_NAME), context.get(LogContext.CLAN_NAME)))
                .material(Material.LIME_DYE)
                .lore(lore)
                .glow(true)
                .frameLore(false)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
