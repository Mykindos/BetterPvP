package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.gui.ScrollGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ScrollItem;
import org.bukkit.Material;

public class ScrollDownItem extends ScrollItem {

    public ScrollDownItem() {
        super(1);
    }

    @Override
    public ItemProvider getItemProvider(ScrollGui<?> gui) {
        ItemBuilder builder = new ItemBuilder(Material.BARRIER).setCustomModelData(1);
        builder.setDisplayName("Scroll down");
        if (!gui.canScroll(1))
            builder.addLoreLines("You've reached the bottom");

        return builder;
    }

}