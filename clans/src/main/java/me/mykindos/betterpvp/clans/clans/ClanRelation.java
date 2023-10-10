package me.mykindos.betterpvp.clans.clans;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.ChatColor;

@Getter
public enum ClanRelation {

    PILLAGE(NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE, MapColor.COLOR_MAGENTA),
    ALLY(NamedTextColor.GREEN, NamedTextColor.DARK_GREEN, MapColor.COLOR_LIGHT_GREEN),
    ALLY_TRUST(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN, MapColor.COLOR_LIGHT_GREEN),
    ENEMY(NamedTextColor.RED, NamedTextColor.DARK_RED, MapColor.COLOR_RED),
    SAFE(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, MapColor.SNOW),
    NEUTRAL(NamedTextColor.YELLOW, NamedTextColor.GOLD, MapColor.GOLD),
    SELF(NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, MapColor.DIAMOND);

    private final NamedTextColor primary;
    private final NamedTextColor secondary;
    private final MapColor materialColor;

    ClanRelation(NamedTextColor primary, NamedTextColor secondary, MapColor materialColor) {
        this.primary = primary;
        this.secondary = secondary;
        this.materialColor = materialColor;
    }


    public Style getPrimary(boolean bold) {
        Style style = Style.style(primary);
        if (bold) {
            style = style.decorate(TextDecoration.BOLD);
        }
        return style;
    }

    public ChatColor getPrimaryAsChatColor(){
        return ChatColor.valueOf(primary.toString().toUpperCase());
    }

    public String getPrimaryMiniColor() {
        return "<" + primary.toString().toLowerCase() + ">";
    }

    public String getSecondaryMiniColor() {
        return "<" + secondary.toString().toLowerCase() + ">";
    }

}