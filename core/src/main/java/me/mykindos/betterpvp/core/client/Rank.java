package me.mykindos.betterpvp.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

@AllArgsConstructor
public enum Rank {
    PLAYER("Player", NamedTextColor.YELLOW, 0),
    HELPER("Helper", NamedTextColor.DARK_GREEN, 1),
    TRIAL_MOD("Trial Mod", NamedTextColor.DARK_AQUA, 2),
    MODERATOR("Mod", NamedTextColor.AQUA, 3),
    ADMIN("Admin", NamedTextColor.RED, 4),
    OWNER("Owner", NamedTextColor.DARK_RED, 5),
    DEVELOPER("", NamedTextColor.WHITE, 6);

    @Getter
    private final String name;

    @Getter
    private final NamedTextColor color;

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
