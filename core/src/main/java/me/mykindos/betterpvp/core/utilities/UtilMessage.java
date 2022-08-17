package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.client.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UtilMessage {

    /**
     * Sends a message to a player with appropriate formatting
     *
     * @param player  The player
     * @param prefix  The message
     * @param message Message to send to a player
     */
    public static void message(Player player, String prefix, Component message) {
        Component prefixComponent = Component.text(ChatColor.BLUE + prefix + "> ");
        player.sendMessage(prefixComponent.append(message));
    }

    /**
     * Sends a message to a CommandSender with appropriate formatting
     * Can also send to players
     *
     * @param sender  The CommandSender
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     */
    public static void message(CommandSender sender, String prefix, String message) {
        sender.sendMessage((!prefix.equals("") ? (ChatColor.BLUE + prefix + "> ") : "") + ChatColor.GRAY + message);
    }

    /**
     * Sends a message to a CommandSender with appropriate formatting
     * Can also send to players
     *
     * @param sender  The CommandSender
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     * @param args    The args to interpolate in the string
     */
    public static void message(CommandSender sender, String prefix, String message, Object... args) {
        sender.sendMessage(String.format(ChatColor.BLUE + prefix + "> " + ChatColor.GRAY + message, args));
    }

    /**
     * Sends a message to a player with appropriate formatting
     * Additionally plays a sound to the player when they receive this message
     *
     * @param player  The player
     * @param prefix  The message
     * @param message Message to send to a player
     * @param sound   Whether or not to send a sound to the player as well
     */
    public static void message(Player player, String prefix, String message, boolean sound) {
        if (sound) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
        }

        player.sendMessage(ChatColor.BLUE + prefix + "> " + ChatColor.GRAY + message);
    }

    /**
     * Sends a message to a player, does not format the message
     *
     * @param player  The player receiving the message
     * @param message The message to be sent
     */
    public static void message(Player player, String message) {
        player.sendMessage(message);
    }


    /**
     * Sends a message to a player, adds the required rank at the end of the message
     *
     * @param player  The player receiving the message
     * @param command The command being executed
     * @param message The message to be sent
     * @param rank    The rank required to use this command
     */
    public static void message(Player player, String command, String message, Rank rank) {
        player.sendMessage(rank.getColor() + command + " " + ChatColor.GRAY + message + rank.getColor() + " " + rank.getTag(false));
    }

    /**
     * Sends an array of strings to a player, does not format the strings
     *
     * @param player  The player receiving the message
     * @param message The strings to be sent
     */
    public static void message(Player player, String[] message) {
        for (String string : message) {
            player.sendMessage(string);
        }
    }

    /**
     * Sends an array of strings to a player with appropriate formatting
     *
     * @param player  The player
     * @param prefix  The message
     * @param message Strings to send to a player
     */
    public static void message(Player player, String prefix, String[] message) {
        for (String string : message) {
            player.sendMessage(ChatColor.BLUE + prefix + "> " + ChatColor.GRAY + string);
        }
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     * @param sender The CommandSender to send the message to
     * @param message The message to send
     */
    public static void simpleMessage(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>" + message));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     */
    public static void simpleMessage(CommandSender sender, String prefix, String message) {
        sender.sendMessage(Component.text(ChatColor.BLUE + prefix + "> ")
                .append(MiniMessage.miniMessage().deserialize("<gray>" + message)));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     * @param args    The args to interpolate in the string
     */
    public static void simpleMessage(CommandSender sender, String prefix, String message, Object... args) {
        sender.sendMessage(Component.text(ChatColor.BLUE + prefix + "> ")
                .append(MiniMessage.miniMessage().deserialize("<gray>" + String.format(message, args))));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param prefix  The prefix of the message
     * @param message The message to be broadcasted
     */
    public static void broadcast(String prefix, String message) {
        Bukkit.getServer().broadcast(Component.text(ChatColor.BLUE + prefix + "> " + ChatColor.GRAY + message));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param message The message to be broadcasted
     */
    public static void broadcast(String message) {
        Bukkit.getServer().broadcast(Component.text(ChatColor.GRAY + message));
    }

}
