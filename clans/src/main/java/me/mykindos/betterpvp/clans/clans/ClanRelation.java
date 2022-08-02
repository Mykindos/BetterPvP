package me.mykindos.betterpvp.clans.clans;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

@Getter
public enum ClanRelation {

    PILLAGE(NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE),
    ALLY(NamedTextColor.GREEN, NamedTextColor.DARK_GREEN),
    ALLY_TRUST(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN),
    ENEMY(NamedTextColor.RED, NamedTextColor.DARK_RED),
    SAFE(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA),
    NEUTRAL(NamedTextColor.YELLOW, NamedTextColor.GOLD),
    SELF(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA);

    private final NamedTextColor primary;
    private final NamedTextColor secondary;

    ClanRelation(NamedTextColor primary, NamedTextColor secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }


    public String getPrimary(boolean bold) {
        return primary.toString() + ChatColor.BOLD;
    }

    public ChatColor getPrimaryAsChatColor(){
        return ChatColor.valueOf(primary.toString().toUpperCase());
    }

}