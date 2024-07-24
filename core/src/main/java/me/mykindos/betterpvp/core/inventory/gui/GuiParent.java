package me.mykindos.betterpvp.core.inventory.gui;

public interface GuiParent {
    
    /**
     * Called by the child {@link Gui} to report an update of a {@link me.mykindos.betterpvp.core.inventory.gui.SlotElement}.
     *
     * @param child     The child {@link Gui} whose {@link me.mykindos.betterpvp.core.inventory.gui.SlotElement} has changed
     * @param slotIndex The slot index of the changed {@link SlotElement} in the child {@link Gui}
     */
    void handleSlotElementUpdate(Gui child, int slotIndex);
    
}
