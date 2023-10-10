package me.mykindos.betterpvp.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@AllArgsConstructor
public enum Rank {
    PLAYER("Player", NamedTextColor.YELLOW,false, 0),
    HELPER("Helper", NamedTextColor.DARK_GREEN, true,1),
    TRIAL_MOD("Trial Mod", NamedTextColor.DARK_AQUA, true,2),
    MODERATOR("Mod", NamedTextColor.AQUA, true,3),
    ADMIN("Admin", NamedTextColor.RED, true,4),
    OWNER("Owner", NamedTextColor.DARK_RED, true,5),
    DEVELOPER("Developer", NamedTextColor.WHITE, false,6);

    @Getter
    private final String name;

    @Getter
    private final NamedTextColor color;

    @Getter
    private final boolean displayPrefix;

    @Getter
    private final int id;


    public Component getTag(boolean bold) {
        Component tag = Component.text(this.name, color);
        if (bold) {
            tag = tag.decorate(TextDecoration.BOLD);
        }
        return tag;
    }

    public Component getPlayerNameMouseOver (String name) {
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

}
