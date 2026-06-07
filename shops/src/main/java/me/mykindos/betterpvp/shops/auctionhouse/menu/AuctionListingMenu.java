package me.mykindos.betterpvp.shops.auctionhouse.menu;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.menu.button.filter.NameSearchButton;
import me.mykindos.betterpvp.core.menu.button.filter.RaritySearchButton;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.AuctionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class AuctionListingMenu extends AbstractPagedGui<Item> implements Windowed {

    @Getter
    private final Player player;

    private final String title;
    private final AuctionManager auctionManager;
    private final Predicate<Auction> auctionFilter;
    private ItemRarity raritySearch = null;
    private String nameSearch = null;

    public AuctionListingMenu(@NotNull AuctionManager auctionManager, @Nullable Windowed previous, Player player, @Nullable Predicate<Auction> auctionFilter) {
        super(9, 6, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new PageForwardButton()));
        this.player = player;
        this.title = "Auction House";
        this.auctionManager = auctionManager;
        this.auctionFilter = auctionFilter;

        setItem(52, new NameSearchButton(() -> nameSearch, newName -> {
            nameSearch = newName;
            refresh();
        }));
        setItem(51, new RaritySearchButton(() -> raritySearch, newRarity -> raritySearch = newRarity, this::refresh));

        refresh();
    }

    public void refresh() {
        final String queryLower = nameSearch == null ? null : nameSearch.toLowerCase(Locale.ROOT);

        List<Item> activeAuctions = auctionManager.getActiveAuctions().stream()
                .filter(auctionFilter != null ? auctionFilter : auction -> true)
                .filter(auction -> {
                    if (raritySearch == null) return true;
                    return auctionManager.getItemFactory()
                            .fromItemStack(auction.getItemStack())
                            .map(itemInstance -> itemInstance.getRarity() == raritySearch)
                            .orElse(false);
                })
                .filter(auction -> {
                    if (queryLower == null) return true;

                    Component name = auctionManager.getItemFactory()
                            .fromItemStack(auction.getItemStack())
                            .map(itemInstance -> itemInstance.getView().getName())
                            .orElseGet(() -> auction.getItemStack().displayName());

                    String displayName = PlainTextComponentSerializer.plainText()
                            .serialize(name)
                            .toLowerCase(Locale.ROOT);
                    return displayName.contains(queryLower);
                })
                .sorted(Comparator.comparingDouble(Auction::getExpiryTime).reversed())
                .map(auction -> new AuctionButton(auctionManager, auction, player))
                .map(Item.class::cast)
                .toList();
        setContent(activeAuctions);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text(title);
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));

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

}
