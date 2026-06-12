package me.mykindos.betterpvp.core.logging.formatters.clans;

import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.locale.Translations;
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
public class ClanPromoteMemberLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "CLAN_PROMOTE";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Translations.component("core.log.clan-promote-member.1",
                Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW),
                Component.text(context.get(LogContext.TARGET_CLIENT_NAME), NamedTextColor.YELLOW),
                Component.text(context.get(LogContext.CURRENT_CLAN_RANK), NamedTextColor.GREEN),
                Component.text(context.get(LogContext.NEW_CLAN_RANK), NamedTextColor.GREEN));
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
                Translations.component("core.log.clan-promote-member.2").color(NamedTextColor.GRAY),
                Component.text(context.get(LogContext.TARGET_CLIENT_NAME), NamedTextColor.YELLOW),
                Translations.component("core.log.clan-promote-member.3",
                        Component.text(context.get(LogContext.CURRENT_CLAN_RANK), NamedTextColor.GREEN),
                        Component.text(context.get(LogContext.NEW_CLAN_RANK), NamedTextColor.GREEN)),
                Component.text(context.get(LogContext.CLAN_NAME), NamedTextColor.AQUA),
                UtilMessage.DIVIDER

        );

        ItemProvider itemProvider = ItemView.builder()
                .displayName(Translations.component("core.log.clan-promote-member.4",
                        Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW),
                        Translations.component("core.log.clan-promote-member.5").color(NamedTextColor.GREEN),
                        Component.text(context.get(LogContext.TARGET_CLIENT_NAME), NamedTextColor.YELLOW)))
                .material(Material.LIME_CANDLE)
                .lore(lore)
                .frameLore(false)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
