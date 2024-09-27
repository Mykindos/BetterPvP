package me.mykindos.betterpvp.core.logging.formatters.items;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.logging.menu.LogRepositoryMenu;
import me.mykindos.betterpvp.core.logging.menu.button.LogRepositoryButton;
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
public class InventoryMoveItemLogFormatter implements ILogFormatter {

    @Override
    public String getAction() {
        return "ITEM_INVENTORY_MOVE";
    }

    @Override
    public Component formatLog(HashMap<String, String> context) {
        return Component.empty().append(Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(context.get(LogContext.ITEM)))))
                .append(Component.text(" was moved from ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.CURRENT_LOCATION), NamedTextColor.YELLOW))
                .append(Component.text(" to ", NamedTextColor.GRAY))
                .append(Component.text(context.get(LogContext.NEW_LOCATION), NamedTextColor.YELLOW));

    }

    @Override
    public Description getDescription(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        HashMap<String, String> context = cachedLog.getContext();
        List<Component> lore = List.of(
                UtilMessage.DIVIDER,
                cachedLog.getRelativeTimeComponent(),
                cachedLog.getAbsoluteTimeComponent(),
                UtilMessage.DIVIDER,
                Component.text(context.get(LogContext.ITEM_NAME), NamedTextColor.GREEN),
                UtilMessage.deserialize("(<light_purple>%s</light_purple>)",
                        context.get(LogContext.ITEM)),
                Component.text("was moved from ", NamedTextColor.GRAY)
                        .append(Component.text(context.get(LogContext.CURRENT_LOCATION), NamedTextColor.GREEN)),
                UtilMessage.deserialize("to <yellow>%s</yellow>",
                        context.get(LogContext.NEW_LOCATION)),
                UtilMessage.DIVIDER
        );

        List<? extends LogRepositoryButton> buttons = List.of(
                new UUIDItemButton(context.get(LogContext.ITEM_NAME), context.get(LogContext.ITEM), JavaPlugin.getPlugin(Core.class), logRepository, previous)
        );

        Material material;
        try {
            material = Material.valueOf(LogContext.BLOCK);
        } catch (IllegalArgumentException ignored) {
            material = Material.HOPPER;
        }

        ItemProvider itemProvider = ItemView.builder()
                .displayName(Component.text("Inventory Move Item"))
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
