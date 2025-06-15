package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.PageItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.function.Supplier;

public class PreviousButton extends PageItem {

    private final Supplier<ItemProvider> itemProvider;

    public PreviousButton() {
        super(false);
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.RED_STAINED_GLASS_PANE);
        itemProvider = builder::build;
    }

    public PreviousButton(Supplier<ItemProvider> builder) {
        super(false);
        this.itemProvider = builder;
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {
        final ItemView.ItemViewBuilder builder = ItemView.of(itemProvider.get().get()).toBuilder();
        builder.clearLore();
        if (gui.hasPreviousPage()) {
            builder.displayName(UtilMessage.deserialize("<red>Previous Page <gray>(<white>%d</white>/%d)", gui.getCurrentPage(), gui.getPageAmount()));
        } else {
            builder.displayName(Component.text("No previous page", NamedTextColor.RED));
        }
        return builder.build();
    }
}
