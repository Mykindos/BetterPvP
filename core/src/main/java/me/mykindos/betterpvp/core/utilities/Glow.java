package me.mykindos.betterpvp.core.utilities;

import fr.skytasul.glowingentities.GlowingBlocks;
import fr.skytasul.glowingentities.GlowingEntities;
import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
public class Glow {

    private static GlowingEntities entities;
    private static GlowingBlocks blocks;

    public static GlowingEntities entities() {
        if (entities == null) {
            entities = new GlowingEntities(JavaPlugin.getPlugin(Core.class));
        }
        return entities;
    }

    public static GlowingBlocks blocks() {
        if (blocks == null) {
            blocks = new GlowingBlocks(JavaPlugin.getPlugin(Core.class));
        }
        return blocks;
    }

    @SuppressWarnings("deprecation")
    public static ChatColor color(NamedTextColor color) {
        for (ChatColor value : ChatColor.values()) {
            if (NamedTextColor.nearestTo(TextColor.color(value.asBungee().getColor().getRGB())) == color) {
                return value;
            }
        }
        throw new IllegalArgumentException("No color found for " + color);
    }

}
