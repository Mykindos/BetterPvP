package me.mykindos.betterpvp.shops.shops.menus;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.filter.NameSearchButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.shops.shops.menus.buttons.OpenSellAllButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.ShopMenuItemButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SortButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.direction.ShopPageButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
@Getter
public class ShopMenu extends AbstractPagedGui<IShopItem> implements Windowed {

    private final String title;
    private final List<IShopItem> shopItems;
    private final ShopContext context;
    @Setter
    private SortMode sortMode = SortMode.ORDER;
    @Setter
    private String nameFilter;

    public ShopMenu(String title, List<IShopItem> shopItems, ShopContext context) {
        super(9, 6, false, new Structure(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "A 0 < ( F ) > 0 B")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new ShopPageButton(false, false))
                .addIngredient('(', new ShopPageButton(false, true))
                .addIngredient(')', new ShopPageButton(true, false))
                .addIngredient('>', new ShopPageButton(true, true)));
        this.title = title;
        this.shopItems = List.copyOf(shopItems);
        this.context = context;

        // we have to do this because we want to pass this
        setItem(45, new SortButton(this));
        setItem(49, new NameSearchButton(() -> nameFilter, filter -> {
            nameFilter = filter;
            refresh();
        }));
        setItem(53, new OpenSellAllButton(this));
        refresh();
    }

    public void refresh() {
        List<IShopItem> result = shopItems.stream()
                .filter(shopItem -> {
                    if (nameFilter == null) return true;
                    String key = shopItem.getItemKey();
                    String keyPart = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
                    return keyPart.contains(nameFilter) || UtilFormat.isSimilar(keyPart, nameFilter, 0.75);
                })
                .sorted(sortMode.getComparator())
                .toList();
        setContent(result);
    }

    public boolean containsShopItem(IShopItem target) {
        return shopItems.stream().anyMatch(shopItem -> shopItem.getId() == target.getId());
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (IShopItem item : content) {
            page.add(new SlotElement.ItemSlotElement(new ShopMenuItemButton(this, item)));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-13><glyph:menu_shop_browse>").font(NEXO);
    }
}
