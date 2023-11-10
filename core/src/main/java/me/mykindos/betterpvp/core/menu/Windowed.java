package me.mykindos.betterpvp.core.menu;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

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
     * @return the {@link Window} that was opened.
     */
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

}
