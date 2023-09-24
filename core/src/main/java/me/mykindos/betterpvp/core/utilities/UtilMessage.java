package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.client.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UtilMessage {

    private static final TagResolver tagResolver = TagResolver.resolver(
            TagResolver.resolver("alt", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("alt2", Tag.styling(NamedTextColor.YELLOW))
    );


    /**
     * Sends a message to a player with appropriate formatting
     *
     * @param sender  The player
     * @param prefix  The message
     * @param message Message to send to a player
     */
    public static void message(CommandSender sender, String prefix, Component message) {
        sender.sendMessage(getPrefix(prefix).append(normalize(message)));
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
       message(sender, prefix, MiniMessage.miniMessage().deserialize(message, tagResolver));
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
        message(sender, prefix, String.format(message, args));
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

        message(player, prefix, message);
    }

    /**
     * Sends a message to a player, does not format the message
     *
     * @param player  The player receiving the message
     * @param message The message to be sent
     */
    public static void message(Player player, String message) {
        player.sendMessage(Component.text(message));
    }

    /**
     * Sends a message to a player, does not format the message
     *
     * @param player  The player receiving the message
     * @param message The message to be sent
     */
    public static void message(Player player, Component message) {
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
        final TextComponent prefixCmpt = Component.text(command, rank.getColor());
        final TextComponent messageCmpt = Component.text(message, NamedTextColor.GRAY);
        final Component rankCmpt = rank.getTag(false);
        player.sendMessage(Component.join(JoinConfiguration.separator(Component.space()), prefixCmpt, messageCmpt, rankCmpt));
    }

    /**
     * Sends an array of strings to a player, does not format the strings
     *
     * @param player  The player receiving the message
     * @param message The strings to be sent
     */
    public static void message(Player player, String[] message) {
        for (String string : message) {
            message(player, string);
        }
    }

    /**
     * Sends an array of strings to a player, does not format the strings
     *
     * @param player  The player receiving the message
     * @param message The strings to be sent
     */
    public static void message(Player player, Component[] message) {
        for (Component string : message) {
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
            message(player, prefix, string);
        }
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender to send the message to
     * @param message The message to send
     */
    public static void simpleMessage(CommandSender sender, String message) {
        sender.sendMessage(deserialize(message));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender
     * @param prefix  The prefix
     * @param message Message to send to the CommandSender
     */
    public static void simpleMessage(CommandSender sender, String prefix, String message) {
        sender.sendMessage(getPrefix(prefix).append(deserialize(message)));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender to send the message to
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     * @param hover   Hover event to add to the message
     */
    public static void simpleMessage(CommandSender sender, String prefix, String message, Component hover) {
        simpleMessage(sender, prefix, deserialize(message), hover);
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender to send the message to
     * @param prefix  The message
     * @param message Message to send to the CommandSender
     * @param hover   Hover event to add to the message
     */
    public static void simpleMessage(CommandSender sender, String prefix, Component message, Component hover) {
        sender.sendMessage(getPrefix(prefix).hoverEvent(HoverEvent.showText(hover)).append(normalize(message)));
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
        simpleMessage(sender, prefix, String.format(message, args));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender
     * @param prefix  The message
     * @param component Message to send to the CommandSender
     */
    public static void simpleMessage(CommandSender sender, String prefix, Component component) {
        sender.sendMessage(getPrefix(prefix).append(normalize(component)));
    }

    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender  The CommandSender
     * @param message Message to send to the CommandSender
     * @param args    The args to interpolate in the string
     */
    public static void simpleMessage(CommandSender sender, String message, Object... args) {
        sender.sendMessage(deserialize(String.format(message, args)));
    }


    public static void simpleBroadcast(String prefix, String message, Object... args) {
        Bukkit.getServer().broadcast(getPrefix(prefix).append(deserialize(String.format(message, args))));
    }

    public static void simpleBroadcast(String prefix, String message, Component hover) {
        Bukkit.getServer().broadcast(getPrefix(prefix).append(deserialize(message)).hoverEvent(HoverEvent.showText(hover)));
    }

    public static Component getMiniMessage(String message, Object... args) {
        return deserialize(String.format(message, args)).decoration(TextDecoration.ITALIC, false);
    }

    public static Component getMiniMessage(String message) {
        return deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static Component deserialize(String message) {
        return normalize(MiniMessage.miniMessage().deserialize(message, tagResolver));
    }

    public static Component deserialize(String message, Object... args) {
        return deserialize(String.format(message, args));
    }

    public static Component normalize(Component component) {
        return component.applyFallbackStyle(NamedTextColor.GRAY);
    }

    public static Component getPrefix(String prefix) {
        if (prefix.isEmpty()) {
            return Component.empty();
        }
        return MiniMessage.miniMessage().deserialize("<blue>" + prefix + "> ");
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param prefix  The prefix of the message
     * @param message The message to be broadcasted
     */
    public static void broadcast(String prefix, String message) {
        Bukkit.getServer().broadcast(getPrefix(prefix).append(deserialize(message)));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param message The message to be broadcasted
     */
    public static void broadcast(String message) {
        Bukkit.getServer().broadcast(deserialize(message));
    }

}
