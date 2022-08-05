package me.mykindos.betterpvp.core.utilities;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class UtilPlayer {

    public static int getPing(Player player) {
        try {

            Method getHandleMethod = player.getClass().getDeclaredMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(player);
            Field pingField = entityPlayer.getClass().getDeclaredField("ping");
            pingField.setAccessible(true);

            int ping = pingField.getInt(entityPlayer);

            return Math.max(ping, 0);
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isCreativeOrSpectator(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
        }
        return false;
    }

    public static boolean isHoldingItem(Player player, Material[] items) {
        return Arrays.stream(items).anyMatch(item -> item == player.getInventory().getItemInMainHand().getType());
    }


}
