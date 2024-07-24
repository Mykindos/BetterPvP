package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link AbstractSingleWindow} that uses a chest/dropper/hopper inventory as the upper inventory.
 * <p>
 * Use the builder obtained by {@link Window#single()}, to get an instance of this class.
 */
final class NormalSingleWindowImpl extends AbstractSingleWindow {
    
    public NormalSingleWindowImpl(
        @NotNull Player player,
        @Nullable ComponentWrapper title,
        @NotNull AbstractGui gui,
        boolean closeable
    ) {
        super(player, title, gui, InventoryUtils.createMatchingInventory(gui, ""), closeable);
    }
    
    public static final class BuilderImpl
        extends AbstractBuilder<Window, Builder.Normal.Single>
        implements Builder.Normal.Single
    {
        
        @Override
        public @NotNull Window build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (guiSupplier == null)
                throw new IllegalStateException("Gui is not defined.");
            
            var window = new NormalSingleWindowImpl(
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
