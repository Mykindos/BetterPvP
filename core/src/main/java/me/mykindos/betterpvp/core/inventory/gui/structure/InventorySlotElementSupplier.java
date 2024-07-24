package me.mykindos.betterpvp.core.inventory.gui.structure;

import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement.InventorySlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.Inventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class InventorySlotElementSupplier implements Supplier<InventorySlotElement> {
    
    private final Inventory inventory;
    private final ItemProvider background;
    private int slot = -1;
    
    public InventorySlotElementSupplier(@NotNull Inventory inventory) {
        this.inventory = inventory;
        this.background = null;
    }
    
    public InventorySlotElementSupplier(@NotNull Inventory inventory, @Nullable ItemProvider background) {
        this.inventory = inventory;
        this.background = background;
    }
    
    @NotNull
    @Override
    public SlotElement.InventorySlotElement get() {
        if (++slot == inventory.getSize()) slot = 0;
        return new InventorySlotElement(inventory, slot, background);
    }
    
}
