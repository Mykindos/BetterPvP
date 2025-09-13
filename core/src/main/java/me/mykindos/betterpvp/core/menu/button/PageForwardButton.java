package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.PageItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.function.Supplier;

public class PageForwardButton extends PageItem {

    private final Supplier<ItemProvider> itemProvider;

    public PageForwardButton() {
        super(true);
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.GREEN_STAINED_GLASS_PANE);
        itemProvider = builder::build;
    }

    public PageForwardButton(Supplier<ItemProvider> builder) {
        super(true);
        this.itemProvider = builder;
    }

    public static PageForwardButton invisible() {
        return new PageForwardButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .build());
    }

    public static PageForwardButton defaultTexture() {
        return new PageForwardButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/page_forward"))
                .build());
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {
        final ItemView.ItemViewBuilder builder = ItemView.of(itemProvider.get().get()).toBuilder();
        builder.clearLore();
        if (gui.hasNextPage()) {
            builder.displayName(UtilMessage.deserialize("<green>Next Page <gray>(<white>%d</white>/%d)", gui.getCurrentPage() + 2, gui.getPageAmount()));
        } else {
            builder.displayName(Component.text("No next page", NamedTextColor.RED));
        }
        return builder.build();
    }
}