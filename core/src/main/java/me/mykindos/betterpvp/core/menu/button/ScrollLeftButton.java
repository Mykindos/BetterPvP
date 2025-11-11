package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.gui.ScrollGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.PageItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ScrollItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.function.Supplier;

public class ScrollLeftButton extends ScrollItem {

    private final Supplier<ItemProvider> itemProvider;

    public ScrollLeftButton() {
        super(-1);
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.RED_STAINED_GLASS_PANE);
        itemProvider = builder::build;
    }

    public ScrollLeftButton(Supplier<ItemProvider> builder) {
        super(-1);
        this.itemProvider = builder;
    }

    public static ScrollLeftButton invisible() {
        return new ScrollLeftButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .build());
    }

    public static ScrollLeftButton defaultTexture() {
        return new ScrollLeftButton(() -> ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/page_backward"))
                .build());
    }

    @Override
    public ItemProvider getItemProvider(ScrollGui<?> gui) {
        final ItemView.ItemViewBuilder builder = ItemView.of(itemProvider.get().get()).toBuilder();
        builder.clearLore();
        if (gui.canScroll(-1)) {
            builder.displayName(Component.text("Scroll Left", NamedTextColor.WHITE));
        } else {
            builder.displayName(Component.text("Can't scroll further", NamedTextColor.RED));
        }
        return builder.build();
    }
}
