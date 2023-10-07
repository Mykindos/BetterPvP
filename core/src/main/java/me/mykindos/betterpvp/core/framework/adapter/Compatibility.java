package me.mykindos.betterpvp.core.framework.adapter;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class Compatibility {

    /**
     * Whether the server is running MythicMobs
     */
    public static boolean MYTHIC_MOBS = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;

    /**
     * Whether the server is running ItemsAdder
     */
    public static boolean ITEMS_ADDER = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;

}
