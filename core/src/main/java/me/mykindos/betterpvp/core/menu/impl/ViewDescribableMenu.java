package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.List;

public class ViewDescribableMenu extends ViewCollectionMenu {

    public ViewDescribableMenu(@NotNull String title, @NotNull List<Describable> pool, @NotNull Windowed previous) {
        super(title, pool.stream().map(describable -> {
            final Description description = describable.getDescription();
            return (Item) new SimpleItem(description.getIcon(), description.getClickFunction());
        }).toList(), previous);
    }

}