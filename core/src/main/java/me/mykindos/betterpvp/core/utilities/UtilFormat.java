package me.mykindos.betterpvp.core.utilities;

import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.UUID;

public class UtilFormat {

    private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    public static String formatNumber(int num) {
        return FORMATTER.format(num);
    }

    public static String cleanString(String string) {
        String modified = string.replace("_", " ");
        return WordUtils.capitalizeFully(modified).replace("_", " ");
    }

    public static String getOnlineStatus(String uuid) {
        return getOnlineStatus(UUID.fromString(uuid));
    }
    public static String getOnlineStatus(UUID uuid) {
        return Bukkit.getPlayer(uuid) == null ? "<red>" : "<green>";
    }

    /**
     * Since some plugins and Lunar client have an inbuilt 'ping when mentioned' feature, this was causing pings every time a player typed
     * This change prevents the ping from triggering off the players own messages, but still works when somebody else says their name
     * @param name The players name
     * @return The name with a ZWNJ character inserted
     */
    public static String spoofNameForLunar(String name) {
        return name.charAt(0) + "\u200C" + name.substring(1);
    }

}
