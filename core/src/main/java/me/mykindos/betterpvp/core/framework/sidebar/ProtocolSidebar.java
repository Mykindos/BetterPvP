package me.mykindos.betterpvp.core.framework.sidebar;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextIterator;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.text.provider.AdventureTextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.text.provider.BungeeCordChatTextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.text.provider.MiniMessageTextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.text.provider.MiniPlaceholdersTextProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.plugin.Plugin;

/**
 * Entry point class for creating new sidebars.
 * <p>
 * This class provides methods for creating new sidebars with different text providers.
 * <p>
 *
 * @author CatCoder
 */
@UtilityClass
public class ProtocolSidebar {

    /**
     * Creates new sidebar with custom text provider.
     *
     * @param title        - sidebar title
     * @param plugin       - plugin instance
     * @param textProvider - text provider
     * @param           - component entity type
     * @return new sidebar
     */
    public Sidebar newSidebar(
            @NonNull TextComponent title,
            @NonNull Plugin plugin,
            @NonNull TextProvider<TextComponent> textProvider
    ) {
        return new Sidebar(title, plugin, textProvider);
    }

    /**
     * {@inheritDoc}
     */
    public  Sidebar newSidebar(
            @NonNull TextIterator title,
            @NonNull Plugin plugin,
            @NonNull TextProvider textProvider
    ) {
        return new Sidebar(title, plugin, textProvider);
    }


    /**
     * Creates new sidebar with {@see https://docs.advntr.dev/minimessage/api.html MiniMessage} text provider.
     * Adventure are natively supported on Paper 1.16.5+.
     *
     * @param title       - sidebar title
     * @param plugin      - plugin instance
     * @param miniMessage - MiniMessage instance
     * @return new sidebar
     */
    public Sidebar newMiniMessageSidebar(
            @NonNull String title,
            @NonNull Plugin plugin,
            @NonNull MiniMessage miniMessage
    ) {
        return newSidebar(title, plugin, new MiniMessageTextProvider(miniMessage));
    }

    /**
     * Creates new sidebar with MiniMessage and MiniPlaceholders text provider.
     * For more information about MiniPlaceholders, see {@see https://github.com/MiniPlaceholders/MiniPlaceholders}
     *
     * @param title       - sidebar title
     * @param plugin      - plugin instance
     * @param miniMessage - MiniMessage instance
     * @return new sidebar
     */
    public Sidebar newMiniplaceholdersSidebar(
            @NonNull String title,
            @NonNull Plugin plugin,
            @NonNull MiniMessage miniMessage
    ) {
        return newSidebar(title, plugin, new MiniPlaceholdersTextProvider(miniMessage));
    }

    /**
     * {@inheritDoc}
     */
    public Sidebar newMiniplaceholdersSidebar(
            @NonNull TextIterator title,
            @NonNull Plugin plugin,
            @NonNull MiniMessage miniMessage
    ) {
        return newSidebar(title, plugin, new MiniPlaceholdersTextProvider(miniMessage));
    }

    /**
     * {@inheritDoc}
     */
    public Sidebar newMiniMessageSidebar(
            @NonNull TextIterator title,
            @NonNull Plugin plugin,
            @NonNull MiniMessage miniMessage
    ) {
        return newSidebar(title, plugin, new MiniMessageTextProvider(miniMessage));
    }

}
