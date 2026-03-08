package me.mykindos.betterpvp.core.menu.button;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlaceholderInventorySlot extends SlotElement.InventorySlotElement {

    private final VirtualInventory virtualInventory;

    public PlaceholderInventorySlot(@NotNull VirtualInventory virtualInventory, int slot, @NotNull ItemProvider background) {
        super(virtualInventory, slot, background);
        this.virtualInventory = virtualInventory;
    }

    public PlaceholderInventorySlot(@NotNull VirtualInventory virtualInventory, @NotNull ItemProvider background) {
        this(virtualInventory, 0, background);
    }
}
