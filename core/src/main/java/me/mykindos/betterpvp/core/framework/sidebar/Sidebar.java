package me.mykindos.betterpvp.core.framework.sidebar;

import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Sidebar {

    public static final Component DEFAULT_TITLE = defaultTitle("BetterPvP");

    public static Component defaultTitle(@NotNull String title) {
        return Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    protected final Gamer gamer;

    @Delegate
    private final net.megavex.scoreboardlibrary.api.sidebar.Sidebar wrapped = JavaPlugin.getPlugin(Core.class).getScoreboardLibrary().createSidebar();

    public Sidebar(Gamer gamer, SidebarComponent title, SidebarType sidebarType) {
        this.gamer = gamer;

        Core plugin = JavaPlugin.getPlugin(Core.class);

        var builder = SidebarComponent.builder();
        SidebarBuildEvent sidebarBuildEvent = UtilServer.callEvent(new SidebarBuildEvent(gamer, this, builder, sidebarType));

        var component = sidebarBuildEvent.getBuilder().build();

        ComponentSidebarLayout layout = new ComponentSidebarLayout(title, component);

        new BukkitRunnable() {
            @Override
            public void run() {
                if(wrapped.closed()) {
                    cancel();
                    return;
                }
                layout.apply(wrapped);
            }
        }.runTaskTimerAsynchronously(plugin, 5L, 5L);

        addPlayer(Objects.requireNonNull(gamer.getPlayer()));
    }

    public Sidebar(Gamer gamer, String title, SidebarType type) {
        this(gamer, SidebarComponent.staticLine(defaultTitle("   " + title + "   ")), type);
    }

}
