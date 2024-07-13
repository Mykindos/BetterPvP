package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link AbstractSplitWindow} that uses a chest/dropper/hopper inventory as the upper inventory
 * and the player inventory as the lower inventory.
 * <p>
 * Use the builder obtained by {@link Window#split()}, to get an instance of this class.
 */
final class NormalSplitWindowImpl extends AbstractSplitWindow {
    
    public NormalSplitWindowImpl(
        @NotNull Player player,
        @Nullable ComponentWrapper title,
        @NotNull AbstractGui upperGui,
        @NotNull AbstractGui lowerGui,
        boolean closeable
    ) {
        super(player, title, upperGui, lowerGui, InventoryUtils.createMatchingInventory(upperGui, ""), closeable);
    }
    
    public static final class BuilderImpl
        extends AbstractBuilder<Window, Builder.Normal.Split>
        implements Builder.Normal.Split
    {
        
        @Override
        public @NotNull Window build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (upperGuiSupplier == null)
                throw new IllegalStateException("Upper Gui is not defined.");
            if (lowerGuiSupplier == null)
                throw new IllegalStateException("Lower Gui is not defined.");
            
            var window = new NormalSplitWindowImpl(
                viewer,
                title,
                (AbstractGui) upperGuiSupplier.get(),
                (AbstractGui) lowerGuiSupplier.get(),
                closeable
            );
            
            applyModifiers(window);
            
            return window;
        }
        
    }
    
}
