package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.util.InventoryUtils;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * A PagedSingleWindow that allows adding a pageChangeHandler after window creation
 */
public class PagedSingleWindow extends AbstractSingleWindow {


    public static @NotNull Builder.Normal.Single builder() {
        return new PagedSingleWindow.BuilderImpl();
    }
    /**
     * Creates a new {@link AbstractSingleWindow}.
     *
     * @param viewer    The player that views the window.
     * @param title     The title of the window.
     * @param gui       The gui of the window.
     * @param closeable Whether the window is closeable.
     */
    protected PagedSingleWindow(Player viewer, ComponentWrapper title, AbstractGui gui, boolean closeable) {
        super(viewer, title, gui, InventoryUtils.createMatchingInventory(gui, ""), closeable);
    }

    /**
     * If the GUI is an instance of a PagedGui, then add a pageChangeHandler
     * @param handler the handler to add to the gui
     */
    public void addPageChangeHandler(BiConsumer<Integer, Integer> handler) {
        if (getGui() instanceof PagedGui<?> pagedGui) {
            pagedGui.addPageChangeHandler(handler);
        }
    }

    /**
     * Builds the {@link Window} with the specified viewer.
     * If this method is used, the viewer does not need to be set using {@link #setViewer(Player)}.
     *
     * @param viewer The {@link Player} to build the {@link Window} for.
     * @return The built {@link Window}.
     */
    public static final class BuilderImpl
            extends AbstractBuilder<Window, Builder.Normal.Single>
            implements Builder.Normal.Single {
        /**
         * Builds the {@link Window} with the specified viewer.
         * If this method is used, the viewer does not need to be set using {@link #setViewer(Player)}.
         *
         * @param viewer The {@link Player} to build the {@link Window} for.
         * @return The built {@link Window}.
         */
        @Override
        public @NotNull Window build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (guiSupplier == null)
                throw new IllegalStateException("Gui is not defined.");

            PagedSingleWindow window = new PagedSingleWindow(
                    viewer,
                    title,
                    (AbstractGui) guiSupplier.get(),
                    closeable
            );

            applyModifiers(window);

            return window;
        }
    }
}
