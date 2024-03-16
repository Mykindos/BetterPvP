package me.mykindos.betterpvp.core.framework.sidebar.impl;

import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.sidebar.ProtocolSidebar;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextIterator;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextIterators;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Sidebar {

    public static final TextIterator DEFAULT_TITLE = defaultTitle("BetterPvP");

    @SuppressWarnings("deprecation")
    public static TextIterator defaultTitle(@NotNull String title) {
        return TextIterators.textFade(title,
                ChatColor.WHITE,
                ChatColor.GOLD,
                ChatColor.YELLOW);
    }

    @Delegate
    private final me.mykindos.betterpvp.core.framework.sidebar.Sidebar wrapped = ProtocolSidebar.newAdventureSidebar(DEFAULT_TITLE, JavaPlugin.getPlugin(Core .class));

    public Sidebar() {
        wrapped.updateLinesPeriodically(5L, 0L);
    }

}
