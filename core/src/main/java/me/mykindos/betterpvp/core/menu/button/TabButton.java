package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.TabItem;

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
