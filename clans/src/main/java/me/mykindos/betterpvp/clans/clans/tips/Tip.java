package me.mykindos.betterpvp.clans.clans.tips;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@AllArgsConstructor
public enum Tip {
    ClAN_CREATE("Create Clan", 2, 1,
            Component.text("You can create a clan by running ", NamedTextColor.GRAY)
                    .append(Component.text("/clan create <name>", NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.suggestCommand("/clan create ")).hoverEvent(HoverEvent.showText(Component.text("/clean create")))),
            0),
    CLAN_INVITE("Invite Player", 2, 1,
            Component.text("You can invite a player by running ", NamedTextColor.GRAY)
                    .append(Component.text("/clan invite <player>", NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.suggestCommand("/clan invite ")).hoverEvent(HoverEvent.showText(Component.text("/clan invite")))),
            1),

    CLAN_HELP("Help", 1, 1,
            Component.text("You can see a list of commands by running ", NamedTextColor.GRAY)
                    .append(Component.text("/clan help", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.runCommand("/clan help")).hoverEvent(HoverEvent.showText(Component.text("/clan help")))),
            2);

    @Getter
    private final String name;

    @Getter
    private int weightCategory;

    @Getter
    private int weight;

    @Getter
    private final Component component;

    @Getter
    private final int id;

}
