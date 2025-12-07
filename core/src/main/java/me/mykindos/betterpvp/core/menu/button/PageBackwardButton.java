package me.mykindos.betterpvp.core.menu.button;

import lombok.With;
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

public class PageBackwardButton extends PageItem {

    private final Supplier<ItemProvider> itemProvider;
    @With
    private boolean disabledInvisible = false;

    public PageBackwardButton() {
        super(false);
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.RED_STAINED_GLASS_PANE);
        itemProvider = builder::build;
    }

    public PageBackwardButton(Supplier<ItemProvider> builder) {
        super(false);
        this.itemProvider = builder;
    }

    public PageBackwardButton(Supplier<ItemProvider> itemProvider, boolean disabledInvisible) {
        super(false);
        this.itemProvider = itemProvider;
        this.disabledInvisible = disabledInvisible;
    }

    public static PageBackwardButton invisible() {
        return new PageBackwardButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .build());
    }

    public static PageBackwardButton defaultTexture() {
        return new PageBackwardButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/page_backward"))
                .build());
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {
        if (disabledInvisible && !gui.hasPreviousPage()) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .hideTooltip(true)
                    .build();
        }

        final ItemView.ItemViewBuilder builder = ItemView.of(itemProvider.get().get()).toBuilder();
        builder.clearLore();
        if (gui.hasPreviousPage()) {
            builder.displayName(UtilMessage.deserialize("<green>Previous Page <gray>(<white>%d</white>/%d)", gui.getCurrentPage(), gui.getPageAmount()));
        } else {
            builder.displayName(Component.text("No previous page", NamedTextColor.RED));
        }
        return builder.build();
    }
}
