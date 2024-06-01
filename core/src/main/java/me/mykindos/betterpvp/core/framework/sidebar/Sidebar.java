package me.mykindos.betterpvp.core.framework.sidebar;

import lombok.experimental.Delegate;
import me.catcoder.sidebar.ProtocolSidebar;
import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Sidebar {

    public static final Component DEFAULT_TITLE = defaultTitle("BetterPvP");

    public static Component defaultTitle(@NotNull String title) {
        return Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    @Delegate
    private final me.catcoder.sidebar.Sidebar<Component> wrapped = ProtocolSidebar.newAdventureSidebar(DEFAULT_TITLE, JavaPlugin.getPlugin(Core.class));

    public Sidebar() {
        wrapped.updateLinesPeriodically(5L, 40L);
    }

}
