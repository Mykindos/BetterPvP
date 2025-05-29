package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.gui.ScrollGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ScrollItem;
import org.bukkit.Material;

public class ScrollUpItem extends ScrollItem {

    public ScrollUpItem() {
        super(-1);
    }

    @Override
    public ItemProvider getItemProvider(ScrollGui<?> gui) {
        ItemBuilder builder = new ItemBuilder(Material.BARRIER).setCustomModelData(1);
        builder.setDisplayName("Scroll up");
        if (!gui.canScroll(-1))
            builder.addLoreLines("You've reached the top");

        return builder;
    }

}