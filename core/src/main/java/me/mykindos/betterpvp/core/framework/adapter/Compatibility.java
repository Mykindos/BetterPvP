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

    /**
     * Whether the server is running ModelEngine
     */
    public static boolean MODEL_ENGINE = Bukkit.getPluginManager().getPlugin("ModelEngine") != null;

    /**
     * Whether the server is running Mineplex StudioEngine
     */
    public static boolean MINEPLEX = Bukkit.getPluginManager().getPlugin("StudioEngine") != null;

    /**
     * Whether the server is running Sword Blocking mixin
     */
    public static boolean SWORD_BLOCKING;

    static {
        try {
            Class.forName("me.mykindos.betterpvp.blocking.BlockingPlugin");
            SWORD_BLOCKING = Bukkit.getPluginManager().getPlugin("SwordBlocking") != null;
        } catch (ClassNotFoundException e) {
            SWORD_BLOCKING = false;
        }
    }

}
