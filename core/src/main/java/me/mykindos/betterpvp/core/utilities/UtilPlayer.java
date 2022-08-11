package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UtilPlayer {

    public static List<Player> getNearbyPlayers(Player player, double radius){
        return getNearbyPlayers(player, player.getLocation(), radius, EntityProperty.ALL);
    }

    public static List<Player> getNearbyPlayers(Player player, Location location, double radius, EntityProperty entityProperty) {
        List<Player> players = player.getWorld().getPlayers().stream()
                .filter(worldPlayer -> worldPlayer.getLocation().distance(location) <= radius && !worldPlayer.equals(player))
                .collect(Collectors.toList());
        FetchNearbyEntityEvent<Player> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(player, location, players, entityProperty);
        UtilServer.callEvent(fetchNearbyEntityEvent);

        return fetchNearbyEntityEvent.getEntities();
    }

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

    public static void sendActionBar(Player player, String msg) {

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));


    }

    /**
     * Adds a specified amount of health to a player
     *
     * @param player Player to add health to
     * @param mod    Amount of health to add to the player
     */
    public static void health(Player player, double mod) {
        if (player.isDead()) {
            return;
        }
        double health = player.getHealth() + mod;
        if (health < 0.0D) {
            health = 0.0D;
        }
        if (health > 20.0D) {
            health = 20.0D;
        }
        player.setHealth(health);
    }


}
