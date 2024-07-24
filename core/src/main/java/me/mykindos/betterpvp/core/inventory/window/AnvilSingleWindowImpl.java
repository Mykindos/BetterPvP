package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.InventoryAccess;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.inventory.AnvilInventory;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * An {@link me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow} that uses an {@link AnvilInventory} as the upper inventory.
 * <p>
 * Use the builder obtained by {@link AnvilWindow#single()}, to get an instance of this class.
 */
final class AnvilSingleWindowImpl extends me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow implements AnvilWindow {
    
    private final AnvilInventory anvilInventory;
    
    public AnvilSingleWindowImpl(
        @NotNull Player player,
        @Nullable ComponentWrapper title,
        @NotNull AbstractGui gui,
        @Nullable List<@NotNull Consumer<@NotNull String>> renameHandlers,
        boolean closable
    ) {
        super(player, title, gui, null, closable);
        anvilInventory = InventoryAccess.createAnvilInventory(player, title != null ? title.localized(player) : null, renameHandlers);
        inventory = anvilInventory.getBukkitInventory();
    }
    
    @Override
    protected void setInvItem(int slot, ItemStack itemStack) {
        anvilInventory.setItem(slot, itemStack);
    }
    
    @Override
    protected void openInventory(@NotNull Player viewer) {
        anvilInventory.open();
    }
    
    @Override
    public String getRenameText() {
        return anvilInventory.getRenameText();
    }
    
    public static final class BuilderImpl
        extends AbstractBuilder<AnvilWindow, AnvilWindow.Builder.Single>
        implements AnvilWindow.Builder.Single
    {
        
        private List<Consumer<String>> renameHandlers;
        
        @Override
        public @NotNull BuilderImpl setRenameHandlers(@NotNull List<@NotNull Consumer<String>> renameHandlers) {
            this.renameHandlers = renameHandlers;
            return this;
        }
        
        @Override
        public @NotNull BuilderImpl addRenameHandler(@NotNull Consumer<String> renameHandler) {
            if (renameHandlers == null)
                renameHandlers = new ArrayList<>();
            
            renameHandlers.add(renameHandler);
            return this;
        }
        
        @Override
        public @NotNull AnvilWindow build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (guiSupplier == null)
                throw new IllegalStateException("Gui is not defined.");
            
            var window = new AnvilSingleWindowImpl(
                viewer,
                title,
                (AbstractGui) guiSupplier.get(),
                renameHandlers,
                closeable
            );
            
            applyModifiers(window);
            
            return window;
        }
        
    }
    
}
