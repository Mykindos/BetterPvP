package me.mykindos.betterpvp.clans.clans.tips.types;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public interface ISuggestCommand {
    default Component suggestCommand(String command, String suggestCommand) {
        Component component = Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(suggestCommand))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
        return component;
    }
}
