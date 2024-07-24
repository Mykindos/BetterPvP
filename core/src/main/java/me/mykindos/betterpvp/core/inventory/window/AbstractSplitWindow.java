package me.mykindos.betterpvp.core.inventory.window;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.util.Pair;
import me.mykindos.betterpvp.core.inventory.util.SlotUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A {@link Window} where top and player {@link Inventory} are affected by different {@link Gui Guis}.
 * <p>
 * Only in very rare circumstances should this class be used directly.
 * Instead, use {@link Window#split()} to create such a {@link Window}.
 */
@Getter
public abstract class AbstractSplitWindow extends AbstractDoubleWindow {
    
    private final AbstractGui upperGui;
    private final AbstractGui lowerGui;
    
    /**
     * Creates a new {@link AbstractSplitWindow}.
     *
     * @param player         The {@link Player} that views the window.
     * @param title          The title of the window.
     * @param upperGui       The {@link Gui} of the upper part of the window.
     * @param lowerGui       The {@link Gui} of the lower part of the window.
     * @param upperInventory The {@link Inventory} of the upper part of the window.
     * @param closeable      Whether the window is closeable.
     */
    protected AbstractSplitWindow(Player player, ComponentWrapper title, AbstractGui upperGui, AbstractGui lowerGui, Inventory upperInventory, boolean closeable) {
        super(player, title, upperGui.getSize() + lowerGui.getSize(), upperInventory, closeable);
        this.upperGui = upperGui;
        this.lowerGui = lowerGui;
    }
    
    @Override
    public void handleSlotElementUpdate(Gui child, int slotIndex) {
        redrawItem(child == upperGui ? slotIndex : upperGui.getSize() + slotIndex,
            child.getSlotElement(slotIndex), true);
    }
    
    @Override
    public SlotElement getSlotElement(int index) {
        if (index >= upperGui.getSize()) return lowerGui.getSlotElement(index - upperGui.getSize());
        else return upperGui.getSlotElement(index);
    }
    
    @Override
    protected Pair<AbstractGui, Integer> getWhereClicked(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == getUpperInventory()) {
            return new Pair<>(upperGui, event.getSlot());
        } else {
            int index = SlotUtils.translatePlayerInvToGui(event.getSlot());
            return new Pair<>(lowerGui, index);
        }
    }
    
    @Override
    protected Pair<AbstractGui, Integer> getGuiAt(int index) {
        if (index < upperGui.getSize()) return new Pair<>(upperGui, index);
        else if (index < (upperGui.getSize() + lowerGui.getSize()))
            return new Pair<>(lowerGui, index - upperGui.getSize());
        else return null;
    }
    
    @Override
    public AbstractGui[] getGuis() {
        return new AbstractGui[] {upperGui, lowerGui};
    }
    
    @Override
    protected List<me.mykindos.betterpvp.core.inventory.inventory.Inventory> getContentInventories() {
        List<me.mykindos.betterpvp.core.inventory.inventory.Inventory> inventories = new ArrayList<>();
        inventories.addAll(upperGui.getAllInventories());
        inventories.addAll(lowerGui.getAllInventories());
        return inventories;
    }
    
    /**
     * Builder for {@link AbstractSplitWindow}.
     * <p>
     * This class should only be used directly if you're creating a custom {@link AbstractBuilder} for a custom
     * {@link AbstractSingleWindow} implementation. Otherwise, use the static builder functions in the {@link Window}
     * interface, such as {@link Window#split()} to obtain a builder.
     * 
     * @param <W> The type of the window.
     * @param <S> The type of the builder.
     */
    @SuppressWarnings("unchecked")
    public abstract static class AbstractBuilder<W extends Window, S extends Builder.Double<W, S>>
        extends AbstractWindow.AbstractBuilder<W, S>
        implements Builder.Double<W, S>
    {
        
        /**
         * The {@link Supplier} to receive the upper {@link Gui} from.
         */
        protected Supplier<Gui> upperGuiSupplier;
        /**
         * The {@link Supplier} to receive the lower {@link Gui} from.
         */
        protected Supplier<Gui> lowerGuiSupplier;
        
        @Override
        public @NotNull S setUpperGui(@NotNull Supplier<Gui> guiSupplier) {
            this.upperGuiSupplier = guiSupplier;
            return (S) this;
        }
        
        @Override
        public @NotNull S setUpperGui(@NotNull Gui gui) {
            this.upperGuiSupplier = () -> gui;
            return (S) this;
        }
        
        @Override
        public @NotNull S setUpperGui(@NotNull Gui.Builder<?, ?> builder) {
            this.upperGuiSupplier = builder::build;
            return (S) this;
        }
        
        @Override
        public @NotNull S setLowerGui(@NotNull Supplier<Gui> guiSupplier) {
            this.lowerGuiSupplier = guiSupplier;
            return (S) this;
        }
        
        @Override
        public @NotNull S setLowerGui(@NotNull Gui gui) {
            this.lowerGuiSupplier = () -> gui;
            return (S) this;
        }
        
        @Override
        public @NotNull S setLowerGui(@NotNull Gui.Builder<?, ?> builder) {
            this.lowerGuiSupplier = builder::build;
            return (S) this;
        }
        
    }
    
}
