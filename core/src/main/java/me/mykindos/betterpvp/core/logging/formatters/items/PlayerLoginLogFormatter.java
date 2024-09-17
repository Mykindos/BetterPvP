package me.mykindos.betterpvp.core.logging.formatters.items;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.logging.menu.LogRepositoryMenu;
import me.mykindos.betterpvp.core.logging.menu.button.LogRepositoryButton;
import me.mykindos.betterpvp.core.logging.menu.button.PlayerItemButton;
import me.mykindos.betterpvp.core.logging.menu.button.UUIDItemButton;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

@WithReflection
public class PlayerLoginLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "ITEM_LOGIN";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW)
                .append(Component.text(" logged in with ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(context.get(LogContext.ITEM)))))
                .append(Component.text(" at ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.LOCATION), NamedTextColor.YELLOW));

    }

    @Override
    public Description getDescription(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        HashMap<String, String> context = cachedLog.getContext();
        List<Component> lore = List.of(
                UtilMessage.DIVIDER,
                cachedLog.getRelativeTimeComponent(),
                UtilMessage.DIVIDER,
                Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW),
                Component.text(context.get(LogContext.CLIENT), NamedTextColor.DARK_GRAY),
                Component.text("logged in with", NamedTextColor.GRAY),
                Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN),
                Component.text(context.get(LogContext.ITEM), NamedTextColor.LIGHT_PURPLE),
                Component.text(context.get(LogContext.LOCATION), NamedTextColor.YELLOW),
                UtilMessage.DIVIDER

        );

        List<? extends LogRepositoryButton> buttons = List.of(
                new UUIDItemButton(context.get(LogContext.ITEM_NAME), context.get(LogContext.ITEM), JavaPlugin.getPlugin(Core.class), logRepository, previous),
                new PlayerItemButton(context.get(LogContext.CLIENT_NAME), context.get(LogContext.CLIENT), "Player", JavaPlugin.getPlugin(Core.class), logRepository, previous)
        );

        ItemProvider itemProvider = ItemView.builder()
                .displayName(Component.text("Log In", NamedTextColor.GREEN))
                .material(Material.LIME_BED)
                .lore(lore)
                .frameLore(false)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .clickFunction((click) -> {
                    new LogRepositoryMenu(getAction(), buttons, previous).show(click.getPlayer());
                })
                .build();
    }
}
