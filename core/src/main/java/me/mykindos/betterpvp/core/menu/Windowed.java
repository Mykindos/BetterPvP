package me.mykindos.betterpvp.core.menu;

import lombok.NonNull;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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
     *
     * @return the {@link Window} that was opened.
     */
    @SneakyThrows
    default Window show(@NonNull Player player) {
        if (Bukkit.getServer().isPrimaryThread()) {
            if (!(this instanceof Gui gui)) {
                throw new UnsupportedOperationException("Cannot show a non-Gui menu");
            }

            final Window window = Window.single()
                    .setTitle(new AdventureComponentWrapper(getTitle()))
                    .setViewer(player)
                    .setGui(gui)
                    .build(player);
            JavaPlugin.getPlugin(Core.class).getLogger().info("%s opened GUI %s".formatted(player.getName(), this.getClass().getName()));

            window.open();

            return window;


        }

        JavaPlugin.getPlugin(Core.class).getLogger().severe("Cannot open a menu on a secondary thread! This can lead to very bad things happening!");
        throw new Exception("Cannot open a menu on a secondary thread!");
    }

}
