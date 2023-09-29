package me.mykindos.betterpvp.clans.clans.tips;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static me.mykindos.betterpvp.clans.clans.tips.TipType.COMMAND_SUGGEST;

public enum Tip {
    ClAN_CREATE(2, 1, TipType.COMMAND_SUGGEST, "You can create a clan by running ", "/clan create <name>", "/clan create ", "/clan create <name>", 0),
    CLAN_INVITE(2, 1, TipType.COMMAND_SUGGEST, "You can invite a player by running ", "/clan invite <player>", "/clan invite ", "/clan invite <player>", 1),

    CLAN_HELP(1, 1, TipType.COMMAND_RUN, "You can see a list of commands by running ", "/clan help", "/clan help", "/clan help", 2);

    @Getter
    private int weightCategory;

    @Getter
    private int weight;

    @Getter
    private final Component component;

    @Getter
    private final int id;

    Tip(int weightCategory, int weight, Component component, int id) {
        this.weightCategory = weightCategory;
        this.weight = weight;
        this.component = component;
        this.id = id;
    }

    Tip(int weightCategory, int weight, TipType type, String preCommand, String command, String commandSend, String hoverText, int id) {
        Component component = Component.text(preCommand, NamedTextColor.GRAY);
        switch (type) {
            case COMMAND_SUGGEST -> {
                component = component.append(suggestCommand(command, commandSend, hoverText));
            }
            case COMMAND_RUN -> {
                component = component.append(runCommand(command, commandSend, hoverText));
            }
        }
        this.weightCategory = weightCategory;
        this.weight = weight;
        this.component = component;
        this.id = id;
    }

    private Component suggestCommand(String command, String suggestCommand, String hoverText) {
        Component component = Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(suggestCommand))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        return component;
    }

    private Component runCommand(String command, String runCommand, String hoverText) {
        Component component = Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(runCommand))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        return component;
    }





}
