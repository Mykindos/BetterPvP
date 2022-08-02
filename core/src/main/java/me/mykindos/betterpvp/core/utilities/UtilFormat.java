package me.mykindos.betterpvp.core.utilities;

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
        return Bukkit.getPlayer(uuid) == null ? ChatColor.RED.toString() : ChatColor.GREEN.toString();
    }


}
