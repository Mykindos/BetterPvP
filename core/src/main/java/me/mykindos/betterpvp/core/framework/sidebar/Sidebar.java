package me.mykindos.betterpvp.core.framework.sidebar;

import lombok.experimental.Delegate;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.text.TextIterator;
import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Sidebar {

    public static final TextIterator DEFAULT_TITLE = defaultTitle("BetterPvP");

    public static TextIterator defaultTitle(@NotNull String title) {
        return new DelayedTextIterator(title,
                NamedTextColor.GOLD,
                NamedTextColor.WHITE,
                NamedTextColor.YELLOW);
    }

    @Delegate
    private final me.catcoder.sidebar.Sidebar<Component> wrapped = ProtocolSidebar.newAdventureSidebar(DEFAULT_TITLE, JavaPlugin.getPlugin(Core.class));

    public Sidebar() {
        wrapped.updateLinesPeriodically(5L, 0L);
    }

}
