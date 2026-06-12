package me.mykindos.betterpvp.core.framework.sidebar;

import lombok.CustomLog;
import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@CustomLog
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

        // Scoreboard packets are sent by the scoreboard library directly, bypassing Paper's GlobalTranslator,
        // so translatable lines/title would never resolve. Render each line server-side into the viewer's
        // locale here (no-op for plain, non-translatable components).
        final Player viewer = Objects.requireNonNull(gamer.getPlayer());
        ComponentSidebarLayout layout = new ComponentSidebarLayout(localized(title, viewer), localized(component, viewer));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (wrapped.closed()) {
                    cancel();
                    return;
                }

                try {
                    if (wrapped.players().isEmpty()) {
                        cancel();
                        return;
                    }
                    layout.apply(wrapped);
                } catch (Exception ex) {
                    log.error("Error applying sidebar layout for gamer {} and type {}", gamer, sidebarType, ex);
                } finally {
                    var toRemove = wrapped.players().stream()
                            .filter(player -> player == null || !player.isConnected())
                            .toList();

                    for (var player : toRemove) {
                        wrapped.removePlayer(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 5L, 5L);

        addPlayer(Objects.requireNonNull(gamer.getPlayer()));
    }

    public Sidebar(Gamer gamer, String title, SidebarType type) {
        this(gamer, SidebarComponent.staticLine(defaultTitle("   " + title + "   ")), type);
    }

    /**
     * Wraps a {@link SidebarComponent} so each line it draws is resolved server-side into the viewer's
     * current locale before being handed to the scoreboard library. Plain (non-translatable) lines are
     * returned unchanged by {@link Translations#render}.
     */
    private static SidebarComponent localized(SidebarComponent component, Player viewer) {
        return drawer -> component.draw((line, format) ->
                drawer.drawLine(line == null ? null : Translations.render(line.asComponent(), viewer.locale()), format));
    }

}
