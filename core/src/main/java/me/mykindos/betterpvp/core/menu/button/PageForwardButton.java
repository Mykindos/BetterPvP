package me.mykindos.betterpvp.core.menu.button;

import lombok.With;
import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.PageItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.function.Supplier;

public class PageForwardButton extends PageItem {

    private final Supplier<ItemProvider> itemProvider;
    @With
    private boolean disabledInvisible = false;

    public PageForwardButton() {
        super(true);
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.GREEN_STAINED_GLASS_PANE);
        itemProvider = builder::build;
    }

    public PageForwardButton(Supplier<ItemProvider> builder) {
        super(true);
        this.itemProvider = builder;
    }

    public PageForwardButton(Supplier<ItemProvider> itemProvider, boolean disabledInvisible) {
        super(true);
        this.itemProvider = itemProvider;
        this.disabledInvisible = disabledInvisible;
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
        if (disabledInvisible && !gui.hasNextPage()) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .hideTooltip(true)
                    .build();
        }

        final ItemView.ItemViewBuilder builder = ItemView.of(itemProvider.get().get()).toBuilder();
        builder.clearLore();
        if (gui.hasNextPage()) {
            builder.displayName(Translations.component("core.menu.button.next-page.name").color(NamedTextColor.GREEN)
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(gui.getCurrentPage() + 2, NamedTextColor.WHITE))
                    .append(Component.text("/" + gui.getPageAmount() + ")", NamedTextColor.GRAY)));
        } else {
            builder.displayName(Translations.component("core.menu.button.no-next-page.name").color(NamedTextColor.RED));
        }
        return builder.build();
    }
}