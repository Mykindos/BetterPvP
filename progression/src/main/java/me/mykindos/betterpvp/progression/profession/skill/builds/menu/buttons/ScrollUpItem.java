package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.gui.ScrollGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ScrollItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ScrollUpItem extends ScrollItem {

    public ScrollUpItem() {
        super(-1);
    }

    @Override
    public ItemProvider getItemProvider(ScrollGui<?> gui) {
        final ItemView.ItemViewBuilder builder = ItemView.of(new ItemStack(Material.BARRIER)).toBuilder();
        builder.customModelData(1);
        builder.displayName(Component.text("Scroll up"));
        builder.itemModel(Resources.ItemModel.INVISIBLE);
        builder.action(ClickActions.LEFT_SHIFT, Component.text("Scroll up 5 levels"));
        if (!gui.canScroll(1))
            builder.lore(Component.text("You've reached the top"));

        return builder.build();
    }

}