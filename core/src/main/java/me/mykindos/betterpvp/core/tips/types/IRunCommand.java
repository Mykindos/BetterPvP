package me.mykindos.betterpvp.core.tips.types;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public interface IRunCommand {
    default Component runCommand(String command) {
        return Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
    }
}
