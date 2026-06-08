package me.mykindos.betterpvp.shops.shops.menus.buttons.direction;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.PageItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public class ShopPageButton extends PageItem {

    private final boolean forward;
    private final boolean invisible;

    public ShopPageButton(boolean forward, boolean invisible) {
        super(forward);
        this.forward = forward;
        this.invisible = invisible;
    }

    @Override
    public ItemProvider getItemProvider(me.mykindos.betterpvp.core.inventory.gui.PagedGui<?> gui) {
        boolean enabled = forward ? gui.hasNextPage() : gui.hasPreviousPage();
        String direction = forward ? "forward" : "backward";
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(invisible
                        ? Resources.ItemModel.INVISIBLE
                        : Key.key("betterpvp", "menu/gui/shop/page_" + direction + (enabled ? "" : "_disabled")))
                .displayName(enabled
                        ? Translations.component(forward ? "core.menu.button.next-page.name" : "core.menu.button.previous-page.name").color(NamedTextColor.GREEN)
                        : Translations.component(forward ? "core.menu.button.no-next-page.name" : "core.menu.button.no-previous-page.name").color(NamedTextColor.RED))
                .build();
    }
}
