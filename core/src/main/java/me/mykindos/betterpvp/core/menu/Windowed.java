package me.mykindos.betterpvp.core.menu;

import lombok.NonNull;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

}
