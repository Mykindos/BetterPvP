package me.mykindos.betterpvp.core.inventory.item.impl.controlitem;

import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;

/**
 * A special type of {@link Item} that stores the {@link Gui} in which it is displayed.
 *
 * @param <G> The Gui Type this {@link ControlItem} will control. Not checked when adding it to a Gui.
 */
public abstract class ControlItem<G extends Gui> extends AbstractItem {
    
    private G gui;
    
    public abstract ItemProvider getItemProvider(G gui);
    
    @Override
    public final ItemProvider getItemProvider() {
        return getItemProvider(gui);
    }
    
    public G getGui() {
        return gui;
    }
    
    public void setGui(G gui) {
        if (this.gui == null) {
            this.gui = gui;
        }
    }
    
}
