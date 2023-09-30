package me.mykindos.betterpvp.clans.clans.tips;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public enum Tip {
    ClAN_CREATE(3, 1, "You can create a clan by running ", "/clan create <name>", "/clan create ", "/clan create <name>", 0),
    CLAN_INVITE(2, 1, "You can invite a player by running ", "/clan invite <player>", "/clan invite ", "/clan invite <player>", 1),

    CLAN_HELP(1, 1, "You can see a list of commands by running ", "/clan help", 2),

    CLAN_HOME(2, 1, "You can set a place you can teleport back to in your territory by running ", "/clan sethome", 3),

    CLAN_ENERGY(2, 1, Component.text("You can purchase energy in the shops", NamedTextColor.GRAY), 4);
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

    Tip(int weightCategory, int weight, String preCommand, String command, int id) {
        this.weightCategory = weightCategory;
        this.weight = weight;
        this.component = Component.text(preCommand, NamedTextColor.GRAY).append(runCommand(command));
        this.id = id;
    }

    Tip(int weightCategory, int weight, String preCommand, String command, String suggestCommand, String hoverText, int id) {
        this.weightCategory = weightCategory;
        this.weight = weight;
        this.component =Component.text(preCommand, NamedTextColor.GRAY).append(suggestCommand(command, suggestCommand, hoverText));
        this.id = id;
    }

    private Component suggestCommand(String command, String suggestCommand, String hoverText) {
        Component component = Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(suggestCommand))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        return component;
    }

    private Component runCommand(String command) {
        Component component = Component.text(command, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
        return component;
    }





}
