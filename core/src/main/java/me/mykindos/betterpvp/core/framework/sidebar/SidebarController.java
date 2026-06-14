package me.mykindos.betterpvp.core.framework.sidebar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@BPvPListener
@Singleton
@CustomLog
public class SidebarController implements Listener {

    private final @NotNull ClientManager clientManager;
    private final @NotNull Core core;
    @Getter
    private @NotNull Function<Gamer, Sidebar> defaultProvider = gamer -> null;
    @Getter
    private @NotNull Function<Gamer, Component> hudProvider = gamer -> null;

    @Inject
    private SidebarController(@NotNull ClientManager clientManager, @NotNull Core core) {
        this.clientManager = clientManager;
        this.core = core;
    }

    public void setDefaultProvider(@NotNull Function<@NotNull Gamer, @Nullable Sidebar> provider) {
        this.defaultProvider = provider;
    }

    /**
     * Sets the renderer for {@link SidebarMode#HUD}. The component it returns is composited onto the
     * gamer's {@code BossBarOverlay} each tick, but only while the gamer's mode is {@link SidebarMode#HUD}
     * — the overlay element is self-gating, so callers never have to add/remove it on mode changes.
     */
    public void setHudProvider(@NotNull Function<@NotNull Gamer, @Nullable Component> provider) {
        this.hudProvider = provider;
    }

    public void resetSidebar(@NotNull Gamer gamer) {
        // (Re)build the scoreboard sidebar. Its constructor adds the player as a viewer, so we apply the
        // current mode immediately afterwards to remove the viewer again when the sidebar isn't selected.
        gamer.setSidebar(this.defaultProvider.apply(gamer));
        applyMode(gamer);
    }

    private @Nullable Component renderHud(@NotNull Gamer gamer) {
        if (modeOf(gamer) != SidebarMode.HUD) {
            return null;
        }
        return this.hudProvider.apply(gamer);
    }

    /** Toggles the scoreboard viewer to match the gamer's current mode. The HUD element gates itself. */
    private void applyMode(@NotNull Gamer gamer) {
        final Sidebar sidebar = gamer.getSidebar();
        if (sidebar == null || !gamer.isOnline()) {
            return;
        }

        final Player player = gamer.getPlayer();
        if (player == null) {
            return;
        }

        final boolean showSidebar = modeOf(gamer) == SidebarMode.SIDEBAR;
        UtilServer.runTaskAsync(core, () -> {
            if (showSidebar) {
                sidebar.addPlayer(player);
            } else {
                sidebar.removePlayer(player);
            }
        });
    }

    private SidebarMode modeOf(@NotNull Gamer gamer) {
        final Client client = clientManager.getStoredExact(gamer.getUniqueId()).orElse(null);
        final Object raw = client == null ? null : client.getProperty(ClientProperty.SIDEBAR_MODE).orElse(null);
        return SidebarMode.parse(raw);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Gamer gamer = this.clientManager.search().online(event.getPlayer()).getGamer();

        // A single self-gating HUD overlay element: it renders only while the mode is HUD and otherwise
        // returns null, which BossBarOverlay silently skips. Added once per join; never removed on toggle.
        // Kept out of resetSidebar() because the game module re-runs that on state changes.
        gamer.getBossBarOverlay().add(new DisplayObject<>(this::renderHud));

        resetSidebar(gamer);
    }

    // Client property because it's a global setting, but the sidebar is attached to the gamer
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSave(ClientPropertyUpdateEvent event) {
        if (!event.getProperty().equals(ClientProperty.SIDEBAR_MODE.name())) {
            return; // Skip if not the sidebar property
        }

        final Gamer gamer = event.getContainer().getGamer();
        if (modeOf(gamer) == SidebarMode.SIDEBAR) {
            // Switching to SIDEBAR: rebuild from scratch. The scoreboard's layout task self-cancels once the
            // sidebar has no viewers (see Sidebar's repeating task), which happens whenever the player has been
            // in HUD/DISABLED since join. Re-adding the viewer alone would never restart that task, so the
            // sidebar would stay blank until relog — resetSidebar builds a fresh Sidebar with a live task.
            resetSidebar(gamer);
        } else {
            applyMode(gamer);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Gamer gamer = this.clientManager.search().online(event.getPlayer()).getGamer();
        final Sidebar sidebar = gamer.getSidebar();
        if (sidebar != null) {
            sidebar.close();
        }
    }

}
