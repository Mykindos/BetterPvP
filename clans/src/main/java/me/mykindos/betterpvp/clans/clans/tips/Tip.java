package me.mykindos.betterpvp.clans.clans.tips;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@AllArgsConstructor
public enum Tip {
    ClAN_CREATE("Create Clan", Component.text("You can create a Clan by running /c create <name>", NamedTextColor.GRAY), 10, 10, 0),
    CLAN_INVITE("Invite Player", Component.text("You can invite a player to your Clan by running /c invite <player>"), 2, 1, 1),

    CLAN_HELP("Help", Component.text("You can get a list of commands by running /c help"), 1, 1, 2);
    @Getter
    private final String name;

    @Getter
    private final Component component;

    @Getter
    private int weightCategory;

    @Getter
    private int weight;

    @Getter
    private final int id;

}
