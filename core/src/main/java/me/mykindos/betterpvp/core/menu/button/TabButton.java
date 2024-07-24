package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.TabGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.TabItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;

public class TabButton extends TabItem {

    private final ItemView selected;
    private final ItemView notSelected;
    private final int tab;

    public TabButton(int tab, ItemView selected, ItemView notSelected) {
        super(tab);
        this.selected = selected;
        this.notSelected = notSelected;
        this.tab = tab;
    }

    @Override
    public ItemProvider getItemProvider(TabGui gui) {
        if (gui.getCurrentTab() == tab) {
            return selected;
        } else {
            return notSelected;
        }
    }
}
