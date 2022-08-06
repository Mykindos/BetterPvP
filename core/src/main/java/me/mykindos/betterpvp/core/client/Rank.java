package me.mykindos.betterpvp.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

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


    public String getTag(boolean bold) {
        String tag = this.name;
        if (bold) {
            return getChatColor().toString() + ChatColor.BOLD + fixColors(tag);
        }
        return getChatColor().toString() + fixColors(tag);
    }

    private String fixColors(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private ChatColor getChatColor(){
        return ChatColor.valueOf(color.toString().toUpperCase());
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
