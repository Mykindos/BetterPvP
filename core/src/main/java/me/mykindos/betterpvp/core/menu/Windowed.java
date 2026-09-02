package me.mykindos.betterpvp.core.menu;

import lombok.NonNull;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.inventory.window.WindowManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a menu {@link Window}. The window creation is deferred to the implementation.
 */
public interface Windowed {

    /**
     * @return The title of this menu.
     */
    @NotNull
    Component getTitle();

    /**
     * Open this menu for the given {@link Player}.
     *
     * @return the {@link Window} that was opened.
     */
    @SneakyThrows
    default Window show(@NonNull Player player) {

        if (!(this instanceof Gui gui)) {
            throw new UnsupportedOperationException("Cannot show a non-Gui menu");
        }

        final Window window = Window.single()
                .setTitle(new AdventureComponentWrapper(getTitle()))
                .setViewer(player)
                .setGui(gui)
                .build(player);

        window.open();

        return window;

    }

    /**
     * Open this menu on the next tick, unless something else has opened a window by then.
     * <p>
     * Meant for close handlers. Opening a window closes the current one, so a close handler cannot tell a real
     * close from a forward navigation, and reopening from inside the close event is dropped by the server
     * anyway. A tick later a forward navigation has a window of its own open, and only a real close leaves the
     * player with none.
     */
    default void showAfterClose(@NonNull Player player) {
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            if (player.isOnline() && WindowManager.getInstance().getOpenWindow(player) == null) {
                show(player);
            }
        }, 1L);
    }

}
