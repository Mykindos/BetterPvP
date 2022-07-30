package me.mykindos.betterpvp.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;

@AllArgsConstructor
public enum Rank {
    PLAYER("Player", ChatColor.YELLOW, 0),
    HELPER("Helper", ChatColor.DARK_GREEN, 1),
    TRIAL_MOD("Trial Mod", ChatColor.DARK_AQUA, 2),
    MODERATOR("Mod", ChatColor.AQUA, 3),
    ADMIN("Admin", ChatColor.RED, 4),
    OWNER("Owner", ChatColor.DARK_RED, 5),
    DEVELOPER("", ChatColor.WHITE, 6);

    @Getter
    private final String name;

    @Getter
    private final ChatColor color;

    @Getter
    private final int id;

    public String getTag(boolean bold) {
        String tag = this.name;
        if (bold) {
            return this.color.toString() + ChatColor.BOLD + fixColors(tag);
        }
        return this.color + fixColors(tag);
    }

    private String fixColors(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
