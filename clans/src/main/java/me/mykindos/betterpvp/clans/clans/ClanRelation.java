package me.mykindos.betterpvp.clans.clans;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.material.MaterialColor;
import org.bukkit.ChatColor;

@Getter
public enum ClanRelation {

    PILLAGE(NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE, MaterialColor.COLOR_PURPLE),
    ALLY(NamedTextColor.GREEN, NamedTextColor.DARK_GREEN, MaterialColor.COLOR_LIGHT_GREEN),
    ALLY_TRUST(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN, MaterialColor.COLOR_GREEN),
    ENEMY(NamedTextColor.RED, NamedTextColor.DARK_RED, MaterialColor.COLOR_RED),
    SAFE(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, MaterialColor.SNOW),
    NEUTRAL(NamedTextColor.YELLOW, NamedTextColor.GOLD, MaterialColor.GOLD),
    SELF(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, MaterialColor.DIAMOND);

    private final NamedTextColor primary;
    private final NamedTextColor secondary;
    private final MaterialColor materialColor;

    ClanRelation(NamedTextColor primary, NamedTextColor secondary, MaterialColor materialColor) {
        this.primary = primary;
        this.secondary = secondary;
        this.materialColor = materialColor;
    }


    public String getPrimary(boolean bold) {
        return primary.toString() + ChatColor.BOLD;
    }

    public ChatColor getPrimaryAsChatColor(){
        return ChatColor.valueOf(primary.toString().toUpperCase());
    }

}