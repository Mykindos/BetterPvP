package me.mykindos.betterpvp.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

@AllArgsConstructor
public enum Rank {


    PLAYER("Player",
            NamedTextColor.YELLOW,
            null,
            null,
            List.of("Standard Player"),
            0
    ),
    YOUTUBE("YT",
            NamedTextColor.DARK_RED,
            UtilMessage.deserialize("<white>Y</white><red>T</red>"),
            UtilMessage.StudioPrefix.appendSpace().append(UtilMessage.deserialize("<white>Y</white><red>T</red>")),
            List.of("<white>A BetterPvP Content Creator",
                    "Click to view their channel"),
            1
    ),
    HELPER("Helper",
            NamedTextColor.DARK_GREEN,
            Component.text("Helper", NamedTextColor.DARK_GREEN),
            UtilMessage.StudioPrefix.appendSpace().append(Component.text("Helper", NamedTextColor.DARK_GREEN)),
            List.of("<white>A BetterPvP Helper",
                    "Ask this person for help!"),
            2
    ),
    TRIAL_MOD("Trial Mod",
            NamedTextColor.DARK_AQUA,
            Component.text("T.Mod", NamedTextColor.DARK_AQUA),
            UtilMessage.StudioPrefix.appendSpace().append(Component.text("T. Mod", NamedTextColor.DARK_AQUA)),
            List.of("<white>A BetterPvP Trial Moderator",
                    "This person is trialing to",
                    "be a BetterPvP Moderator.",
                    "You can ask them for help!"),
            3
    ),
    MODERATOR("Mod",
            NamedTextColor.AQUA,
            Component.text("Mod", NamedTextColor.AQUA),
            UtilMessage.StudioPrefix.appendSpace().append(Component.text("Mod", NamedTextColor.AQUA)),
            List.of("A BetterPvP Moderator",
                    "This person is responsible",
                    "For enforcing BetterPvP chat rules",
                    "and to help players!"),
            4
    ),
    MINEPLEX("Mineplex",
            NamedTextColor.GOLD,
            UtilMessage.MineplexPrefix,
            UtilMessage.MineplexPrefix,
            List.of("<white>A Mineplex Staff Member",
                    "This is a Mineplex Staff member"),
            5
    ),
    ADMIN("Admin",
            NamedTextColor.RED,
            Component.text("Admin", NamedTextColor.RED),
            UtilMessage.StudioPrefix.appendSpace().append(Component.text("Admin", NamedTextColor.RED)),
            List.of("<white>A BetterPvP Admin",
                    "This person is a BetterPvP Admin.",
                    "They have more permissions to handle",
                    "problems within BetterPvP",
                    "and typically have more",
                    "responsibilities within the studio"),
            6),
    DEVELOPER("Developer",
            NamedTextColor.WHITE,
            Component.text("Dev", NamedTextColor.WHITE),
            UtilMessage.StudioPrefix.appendSpace().append(Component.text("Dev", NamedTextColor.WHITE)),
            List.of("<white>A BetterPvP Developer",
                    "This person is a BetterPvP Developer.",
                    "They create and maintain the games",
                    "BetterPvP offers on Mineplex"),
            7);

    @Getter
    private final String name;

    @Getter
    private final NamedTextColor color;
    private final Component shortTag;
    private final Component longTag;
    private final List<String> description;

    @Getter
    private final int id;

    public Component getTag(ShowTag type, boolean bold) {
        if (this.equals(PLAYER)) return Component.empty();
        Component componentDescription = description.stream()
                .map(UtilMessage::deserialize)
                .reduce(Component.empty(), (component, component2) -> {
                    if (!component.equals(Component.empty())) {
                        return component.appendNewline().append(component2);
                    }
                    return component2;
                });
        switch (type) {
            case SHORT -> {
                return Component.empty()
                        .append(shortTag
                                .hoverEvent(HoverEvent.showText(componentDescription))
                                .decoration(TextDecoration.BOLD, bold).appendSpace());
            }
            case LONG -> {
                return Component.empty()
                        .append(longTag
                                .hoverEvent(componentDescription)
                                .decoration(TextDecoration.BOLD, bold).appendSpace());
            }
            case null, default -> {
                return Component.empty();
            }
        }
    }

    public Component getPlayerNameMouseOver(String name) {
        return Component.text(name, getColor()).hoverEvent(HoverEvent.showText(Component.text(getName(), getColor())));
    }

    public static Rank getRank(int id) {
        for (Rank rank : Rank.values()) {
            if (rank.getId() == id) {
                return rank;
            }
        }
        return null;
    }

    public enum ShowTag {
        /**
         * Don't show a tag
         */
        NONE,
        /**
         * Show the short version of a tag
         */
        SHORT,
        /**
         * Show a long version of a tag
         */
        LONG
    }

}
