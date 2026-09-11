package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.locale.TranslationService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.tag.CoinsTag;
import me.mykindos.betterpvp.core.utilities.model.tag.DamageTag;
import me.mykindos.betterpvp.core.utilities.model.tag.ExperienceTag;
import me.mykindos.betterpvp.core.utilities.model.tag.HealthTag;
import me.mykindos.betterpvp.core.utilities.model.tag.ManaTag;
import me.mykindos.betterpvp.core.utilities.model.tag.ResistanceTag;
import me.mykindos.betterpvp.core.utilities.model.tag.TimeTag;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilMessage {

    public static final TagResolver tagResolver = TagResolver.resolver(
            TagResolver.standard(),
            TagResolver.resolver("alt", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("alt2", Tag.styling(NamedTextColor.YELLOW)),
            TagResolver.resolver("orange", Tag.styling(TextColor.color(0xFFA500))),
            TagResolver.resolver("val", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("effect", Tag.styling(NamedTextColor.WHITE)),
            TagResolver.resolver("stat", Tag.styling(NamedTextColor.YELLOW)),
            TagResolver.resolver("coins", new CoinsTag()),
            TagResolver.resolver("damage", new DamageTag()),
            TagResolver.resolver("health", new HealthTag()),
            TagResolver.resolver("exp", new ExperienceTag()),
            TagResolver.resolver("mana", new ManaTag()),
            TagResolver.resolver("resistance", new ResistanceTag()),
            TagResolver.resolver("time", new TimeTag())
    );

    public static final MiniMessage miniMessage = MiniMessage.builder().tags(tagResolver).build();

    public static final TextComponent DIVIDER = Component.text("                                            ")
            .color(NamedTextColor.DARK_GRAY)
            .decorate(TextDecoration.STRIKETHROUGH);

    public static final Component StudioPrefix = Component.empty().append(Component.text("BPvP", NamedTextColor.RED));

    /**
     * Sends a message to a player with appropriate formatting
     *
     * @param sender  The player
     * @param prefix  The message
     * @param message Message to send to a player
     */
    public static void message(Audience sender, @Nullable String prefix, Component message) {
        message(sender, translated(prefix), message);
    }

    public static void message(Audience sender, @Nullable ComponentLike prefix, Component message) {
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
    public static void message(Audience sender, String prefix, String message) {
        message(sender, translated(prefix), translated(message));
    }

    public static void message(Audience sender, ComponentLike prefix, String message) {
        sender.sendMessage(getPrefix(prefix).append(resolveStringMessage(message)));
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
    public static void message(Audience sender, String prefix, String message, Object... args) {
        message(sender, prefix, String.format(message, args));
    }

    public static void message(Audience sender, String prefixKey, String key, ComponentLike... args) {
        message(sender, translated(prefixKey), translated(key, args));
    }

    public static void message(Audience sender, Component prefix, String key, ComponentLike... args) {
        message(sender, prefix, translated(key, args));
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
    public static void message(Audience player, String message) {
        player.sendMessage(Component.text(message));
    }

    /**
     * Sends a message to a player, does not format the message
     *
     * @param player  The player receiving the message
     * @param message The message to be sent
     */
    public static void message(Audience player, Component message) {
        player.sendMessage(message);
    }

    public static void message(Audience player, String key, ComponentLike... args) {
        message(player, translated(key, args));
    }


    /**
     * Sends a message to a player, adds the required rank at the end of the message
     *
     * @param player  The player receiving the message
     * @param command The command being executed
     * @param message The message to be sent
     * @param rank    The rank required to use this command
     */
    public static void message(Audience player, String command, String message, Rank rank) {
        final TextComponent prefixCmpt = Component.text(command, rank.getColor());
        final TextComponent messageCmpt = Component.text(message, NamedTextColor.GRAY);
        final Component rankCmpt = rank.getTag(Rank.ShowTag.LONG, false);
        player.sendMessage(Component.join(JoinConfiguration.separator(Component.space()), prefixCmpt, messageCmpt, rankCmpt));
    }


    /**
     * Sends a message utilizing <a href="https://docs.adventure.kyori.net/minimessage">MiniMessage</a> from Adventure API
     *
     * @param sender    The CommandSender
     * @param prefix    The message
     * @param component Message to send to the CommandSender
     */
    public static void simpleMessage(Audience sender, String prefix, Component component) {
        simpleMessage(sender, Component.text(prefix), component);
    }

    public static void simpleMessage(Audience sender, ComponentLike prefix, Component component) {
        sender.sendMessage(getPrefix(prefix).append(normalize(component)));
    }

    // Compatibility overloads for literal (non-translated) messages. Retained for code that has not yet been
    // migrated to the translation-key API; prefer message(sender, prefixKey, key, args...) for new code.
    public static void simpleMessage(Audience sender, String message) {
        sender.sendMessage(deserialize(message));
    }

    public static void simpleMessage(Audience sender, String prefix, String message) {
        sender.sendMessage(getPrefix(prefix).append(deserialize(message)));
    }

    public static void simpleMessage(Audience sender, String prefix, String message, Component hover) {
        simpleMessage(sender, prefix, deserialize(message), hover);
    }

    public static void simpleMessage(Audience sender, String prefix, Component message, Component hover) {
        sender.sendMessage(getPrefix(prefix).hoverEvent(HoverEvent.showText(hover)).append(normalize(message)));
    }

    public static void simpleMessage(Audience sender, String prefix, String message, Object... args) {
        simpleMessage(sender, prefix, String.format(message, args));
    }

    public static void simpleMessage(Audience sender, String message, Object... args) {
        sender.sendMessage(deserialize(String.format(message, args)));
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

        return normalize(miniMessage.deserialize(msg, tagResolver));
    }

    public static Component deserialize(String message, Object... args) {
        return deserialize(String.format(message, args));
    }

    public static String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    /**
     * Creates a component with a click event that copies text to the clipboard when clicked, and a hover event that shows "Click to Copy"
     * @param commandText The text to show in the message, which will have the hover and click events
     * @return A component with a click event that copies text to the clipboard when clicked, and a hover event that shows "Click to Copy"
     */
    public static Component copyCommand(String commandText) {
        return copyCommand(commandText, commandText);
    }

    /**
     * Creates a component with a click event that copies text to the clipboard when clicked, and a hover event that shows "Click to Copy"
     * @param commandText The text to show in the message, which will have the hover and click events. This is usually the command being copied, but can be any text.
     * @param copyText The text that will be copied to the clipboard when the message is clicked. This is usually the command being copied, but can be any text.
     * @return A component with a click event that copies text to the clipboard when clicked, and a hover event that shows "Click to Copy"
     */
    public static Component copyCommand(String commandText, String copyText) {
        return UtilMessage.deserialize("<gold>" + commandText + "</gold>")
                .hoverEvent(HoverEvent.showText(Translations.component("core.util.copy_command.hover")))
                .clickEvent(ClickEvent.copyToClipboard(copyText));
    }

    /**
     * Creates a component with a click event that changes the page of a book when clicked, and a hover event that shows "Click to open page X"
     * @param entryText The text to show in the message, which will have the hover and click events. This is usually the name of the section of the book that this entry corresponds to, but can be any text.
     * @param pageNum The page number that the book will change to when the message is clicked. This is usually the page that the section of the book that this entry corresponds to starts on, but can be any page number.
     * @return A component with a click event that changes the page of a book when clicked, and a hover event that shows "Click to open page X"
     */
    public static Component tableOfContentsEntry(String entryText, int pageNum) {
        return Component.empty().append(UtilMessage.deserialize("<reset><black>" + entryText + ": " + pageNum).decoration(TextDecoration.BOLD, false)
                .hoverEvent(HoverEvent.showText(Translations.component("core.util.table_of_contents.hover", Component.text(pageNum))))
                .clickEvent(ClickEvent.changePage(pageNum)));
    }

    public static Component normalize(Component component) {
        return component.applyFallbackStyle(NamedTextColor.GRAY);
    }

    public static Component getPrefix(String prefix) {
        return getPrefix(Component.text(prefix));
    }

    public static Component getPrefix(@Nullable ComponentLike prefix) {

        if (prefix == null) {
            return Component.empty();
        }

        Component prefixComponent = prefix.asComponent();
        return prefixComponent.color(NamedTextColor.BLUE).append(Component.text("> ", NamedTextColor.BLUE));
    }

    /**
     * Broadcasts a message to all players on the server with formatting
     *
     * @param prefix  The PREFIX of the message
     * @param key The message to be broadcasted
     * @param args    The args to interpolate in the string
     */
    public static void broadcast(String prefix, String key, ComponentLike... args) {
        // Treat prefix as a translation key
        Bukkit.getServer().broadcast(getPrefix(translated(prefix)).append(normalize(translated(key, args))));
    }


    /**
     * Broadcasts a translated message using translated prefix and message keys.
     *
     * @param prefixKey  The translation key for the prefix (e.g. "events.prefix")
     * @param messageKey The translation key for the message
     */
    public static void broadcast(String prefixKey, String messageKey) {
        Bukkit.getServer().broadcast(getPrefix(translated(prefixKey)).append(normalize(translated(messageKey))));
    }

    // Note: the (String prefixKey, String messageKey, ComponentLike... args) variant is handled by
    // broadcast(String, String, ComponentLike...) above which now translates the prefix too.

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

    /**
     * Broadcasts a message to every online player, building the component <b>per recipient</b> so that any
     * embedded item-hover events can be localized into that recipient's locale.
     *
     * <p>Use this (not {@link #broadcast(Component)}) whenever a broadcast message embeds an item hover:
     * item hover data is serialized into the chat packet and is not resolved by the client, so the hover
     * ItemStack must be rendered server-side for each viewer (see
     * {@link Translations#renderItemStack(org.bukkit.inventory.ItemStack, Locale)}).</p>
     *
     * @param messageForLocale builds the message for a given recipient locale
     */
    public static void broadcastLocalized(Function<Locale, Component> messageForLocale) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(messageForLocale.apply(player.locale()));
        }
    }

    /**
     * Broadcasts a pre-built component to all players on the server with a formatted prefix
     *
     * @param prefix  The PREFIX of the message
     * @param message The component to be broadcasted
     */
    public static void broadcast(String prefix, Component message) {
        // Treat prefix as a translation key
        Bukkit.getServer().broadcast(getPrefix(translated(prefix)).append(normalize(message)));
    }

    public static void broadcast(ComponentLike prefix, Component message) {
        Bukkit.getServer().broadcast(getPrefix(prefix).append(normalize(message)));
    }

    // The (String, Component) overload now translates the prefix by default.

    private static Component translated(String key, ComponentLike... args) {
        if(key == null || key.isEmpty()) {
            return Component.empty();
        }

        return normalize(Translations.component(key, args));
    }

    private static Component resolveStringMessage(String message) {
        if (TranslationService.translator().hasTranslation(message)) {
            return translated(message);
        }

        return deserialize(message);
    }

}
