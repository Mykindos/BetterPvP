package me.mykindos.betterpvp.core.framework;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class ServerTypes {

    public static final ClansServerType CLANS_CLASSIC = new ClansServerType(
            "Clans Classic", 6, "clans",
            Component.text("Clans Classic", NamedTextColor.RED, TextDecoration.BOLD));

    public static final ClansServerType CLANS_SQUADS = new ClansServerType(
            "Clans Squads", 4, "squads",
            Component.text("Clans Squads", NamedTextColor.AQUA, TextDecoration.BOLD));

    public static final ClansServerType CLANS_CASUAL = new ClansServerType(
            "Clans Casual", 6, "casual",
            Component.text("Clans Casual", NamedTextColor.GREEN, TextDecoration.BOLD));

    public static final ChampionsServerType CHAMPIONS = new ChampionsServerType(
            "Champions", "champions",
            Component.text("Champions", NamedTextColor.AQUA, TextDecoration.BOLD));

    public static final ServerType HUB = new BaseServerType("Hub");

    private ServerTypes() {
    }
}
