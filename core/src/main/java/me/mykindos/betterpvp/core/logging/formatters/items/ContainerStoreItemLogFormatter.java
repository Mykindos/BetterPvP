package me.mykindos.betterpvp.core.logging.formatters.items;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.logging.menu.LogRepositoryMenu;
import me.mykindos.betterpvp.core.logging.menu.button.LocationButton;
import me.mykindos.betterpvp.core.logging.menu.button.PlayerItemButton;
import me.mykindos.betterpvp.core.logging.menu.button.UUIDItemButton;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.PreviousableButton;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
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
public class ContainerStoreItemLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "ITEM_CONTAINER_STORE";
    }


    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW)
                .append(Component.text(" stored ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(context.get(LogContext.ITEM)))))
                .append(Component.text(" in ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.BLOCK), NamedTextColor.YELLOW))
                .append(Component.text(" at ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.LOCATION), NamedTextColor.YELLOW));

    }

    @Override
    public Description getDescription(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        HashMap<String, String> context = cachedLog.getContext();
        List<Component> lore = List.of(
                UtilMessage.DIVIDER,
                cachedLog.getRelativeTimeComponent(),
                cachedLog.getAbsoluteTimeComponent(),
                UtilMessage.DIVIDER,
                Component.text(context.get(LogContext.CLIENT_NAME), NamedTextColor.YELLOW)
                        .append(Component.text(" stored", NamedTextColor.GRAY)),
                Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN),
                UtilMessage.deserialize("(<light_purple>%s</light_purple>)",
                        context.get(LogContext.ITEM)),
                Component.text("in ", NamedTextColor.GRAY)
                        .append(Component.text(context.get(LogContext.BLOCK) == null ? "NULL" : context.get(LogContext.BLOCK), NamedTextColor.GREEN)),
                UtilMessage.deserialize("at <yellow>%s</yellow>",
                        context.get(LogContext.LOCATION)),
                UtilMessage.DIVIDER
        );

        List<? extends PreviousableButton> buttons = List.of(
                new UUIDItemButton(context.get(LogContext.ITEM_NAME), context.get(LogContext.ITEM), JavaPlugin.getPlugin(Core.class), logRepository, previous),
                new PlayerItemButton(context.get(LogContext.CLIENT_NAME), context.get(LogContext.CLIENT), "Storer", JavaPlugin.getPlugin(Core.class), logRepository, previous),
                new LocationButton(UtilWorld.stringToLocation(context.get(LogContext.LOCATION)), true, previous)
        );

        Material material;
        try {
            material = Material.valueOf(LogContext.BLOCK);
        } catch (IllegalArgumentException ignored) {
            material = Material.CHEST;
        }

        ItemProvider itemProvider = ItemView.builder()
                .displayName(Component.text("Container Store Item"))
                .material(material)
                .lore(lore)
                .glow(true)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .clickFunction((click) -> {
                    new LogRepositoryMenu(getAction(), buttons, previous).show(click.getPlayer());
                })
                .build();
    }
}
