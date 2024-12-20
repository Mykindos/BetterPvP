package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.examination.Examinable;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.min;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilMessage {

    private static final TagResolver tagResolver = TagResolver.resolver(
            TagResolver.resolver("alt", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("alt2", Tag.styling(NamedTextColor.YELLOW)),
            TagResolver.resolver("orange", Tag.styling(TextColor.color(0xFFA500)))
    );

    public static final TextComponent DIVIDER = Component.text("                                            ")
            .color(NamedTextColor.DARK_GRAY)
            .decorate(TextDecoration.STRIKETHROUGH);


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
     * @param sender    The CommandSender
     * @param prefix    The message
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
        String msg = message;
        if(msg.contains(String.valueOf(UtilFormat.COLOR_CHAR))) {
            msg = UtilFormat.stripColor(message);
        }

        return normalize(MiniMessage.miniMessage().deserialize(msg, tagResolver));
    }

    public static Component deserialize(String message, Object... args) {
        return deserialize(String.format(message, args));
    }

    public static String infoAboutComponent(Component component) {
        String information = component.examinableName();
        if (component instanceof TextComponent textComponent) {
            information += "\nText Component";
            information += "\nContent: " + textComponent.content();
        }
        if (component.hasStyling()) {
            information += "\nStyle: " + component.style().toString();
        }
        information += "\nChildren (" + component.children().size() + ")";
        for (Component child : component.children()) {
            information += "\n" + infoAboutComponent(child);
        }
        return information;
    }

    /**
     * Turns a text component into a list of TextComponents, each 1 character long, with the same style
     * @param component the text component to flatten
     * @return A list of TextComponents 1 character long
     */
    public static List<TextComponent> flattenComponent(TextComponent component) {
        List<TextComponent> components = new ArrayList<>();
        Style style = component.style();
        for (char c : component.content().toCharArray()) {
            components.add(Component.text()
                    .content(String.valueOf(c))
                    .style(style)
                    .build());
        }
        for (Component child : component.children()) {
            components.addAll(flattenComponent((TextComponent) child));
        }

        return components;
    }

    //TODO ignore whitespace if new line starts with it
    //TODO handle remainder (what happens if we end and last break isn't i?)
    public static List<TextComponent> split(TextComponent message, int min, int max) {
        //Bukkit.broadcastMessage("New split, min: " + min + " max: " + max);
        //Bukkit.broadcast(message);

        List<TextComponent> newLines = new ArrayList<>();
        List<TextComponent> elements = flattenComponent(message);

        int currentLineLength = 0;
        int lastSpace = 0;
        int lastBreak = 0;

        for (int i = 0; i < elements.size(); i++) {
            TextComponent component = elements.get(i);
            String content = component.content();

            //Bukkit.broadcast(component);
            //Bukkit.broadcastMessage("i: " + i + " line length: " + currentLineLength + " last space: " + lastSpace + " last break: " + lastBreak);
            int length = content.length();
            if (content.equals("\n")) {
                //Bukkit.broadcastMessage("newline, breaking");
                //this is a line break, so add this to our new component list
                int n = i + 1;
                newLines.add(getComponentFromList(elements, lastBreak, n));
                currentLineLength = 0;
                lastSpace = n;
                lastBreak = n;
                continue;
            }
            if (content.isBlank()) {
                if (currentLineLength == 0) {
                    //ignore whitespace if it is at the beginning of a line
                    //Bukkit.broadcastMessage("space at beginning, ignoring");
                    lastBreak = i + 1;
                    continue;
                }
                //this is a space, set the last space to here
                //Bukkit.broadcastMessage("space");
                lastSpace = i;
            }
            if (currentLineLength + length <= min && currentLineLength + length < max) {
                    //still must be less than max
                    currentLineLength += length;
                    //Bukkit.broadcastMessage("new length is less than equal to min, is now: " + currentLineLength);
                    continue;
                }

            //Bukkit.broadcastMessage("line is greater than min or is greater than max");
            //now greater than the min alloted.
            if (lastSpace == i) {
                //we have hit a space, add a new component and then continue
                //Bukkit.broadcastMessage("found a space, returning");
                int n = i + 1;
                newLines.add(getComponentFromList(elements, lastBreak, n));
                currentLineLength = 0;
                lastSpace = n;
                lastBreak = n;
                continue;
            }
            if (currentLineLength + length >= max) {
                //Bukkit.broadcastMessage("line is at max");
                //we have hit the maximum length of a line
                if (lastSpace <= lastBreak) {
                    //there is no spaces in this line, just cut it off here
                    //Bukkit.broadcastMessage("solid chunk");
                    int n = i + 1;
                    newLines.add(getComponentFromList(elements, lastBreak, n));
                    currentLineLength = 0;
                    lastBreak = n;
                    continue;
                }
                //we split at the most recent space
                //Bukkit.broadcastMessage("Going from last space");
                newLines.add(getComponentFromList(elements, lastBreak, lastSpace));
                currentLineLength = i - (lastSpace);
                lastBreak = lastSpace + 1;
                continue;
            }
            //we have not hit a space or the max, continue incrementing
            //Bukkit.broadcastMessage("incrementing above min");
            currentLineLength += length;
            continue;
        }
        //handle remainder
        newLines.add(getComponentFromList(elements, lastBreak, elements.size()));
        return newLines;
    }

    private static TextComponent getComponentFromList(List<TextComponent> elements, int start, int end) {
        Component component = Component.empty();
        for (int i = start; i < end; i++) {
            component = component.append(elements.get(i));
        }
        return (TextComponent) component.compact();
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
     * @param prefix  The prefix of the message
     * @param message The message to be broadcasted
     * @param args    The args to interpolate in the string
     */
    public static void broadcast(String prefix, String message, Object... args) {
        Bukkit.getServer().broadcast(getPrefix(prefix).append(deserialize(message, args)));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param message The message to be broadcasted
     */
    public static void broadcast(String message) {
        Bukkit.getServer().broadcast(deserialize(message));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param message The message to be broadcasted
     */
    public static void broadcast(Component message) {
        Bukkit.getServer().broadcast(message);
    }

    @Data
    public static class ComponentSplit {
        @Nullable Component current;
        @Nullable Component remainder;
        int usedLength;

        public ComponentSplit(@Nullable Component current, Component remainder, int usedLength) {
            this.current = current;
            this.remainder = remainder;
            this.usedLength = usedLength;
        }
    }

}
