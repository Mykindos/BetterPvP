package me.mykindos.betterpvp.shops.shops.menus;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.items.ShopItem;
import me.mykindos.betterpvp.shops.shops.services.ShopItemSellService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bundles the shared services and display logic needed by shop menus and their child views.
 * Passed down to {@link ShopItemMenu} and {@link SellAllMenu} so they don't need to hold a
 * back-reference to the parent {@link ShopMenu}.
 */
@Getter
public class ShopContext {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final ShopItemSellService sellService;
    private final List<IShopItem> allShopItems;
    private final Map<String, IShopItem> shopItemsByKey;

    public ShopContext(ItemFactory itemFactory, ClientManager clientManager,
                       ShopItemSellService sellService, List<IShopItem> allShopItems) {
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        this.sellService = sellService;
        this.allShopItems = List.copyOf(allShopItems);
        this.shopItemsByKey = sellService.createShopItemIndex(allShopItems);
    }

    public BaseItem getBaseItem(IShopItem shopItem) {
        return Objects.requireNonNull(
                itemFactory.getItemRegistry().getItem(shopItem.getItemKey()),
                "Unknown shop item key " + shopItem.getItemKey());
    }

    public ShopCurrency getCurrency(IShopItem shopItem) {
        if (shopItem instanceof ShopItem castedShopItem && castedShopItem.getItemFlags().containsKey("SHOP_CURRENCY")) {
            try {
                return ShopCurrency.valueOf(castedShopItem.getItemFlags().get("SHOP_CURRENCY").toUpperCase());
            } catch (IllegalArgumentException e) {
                return ShopCurrency.COINS;
            }
        }
        return ShopCurrency.COINS;
    }

    public ItemView createDisplayStack(IShopItem shopItem, int amount) {
        ItemStack item = itemFactory.create(getBaseItem(shopItem)).getView().get();

        ItemView.ItemViewBuilder builder = ItemView.of(item).toBuilder();
        builder.amount(amount);

        // Add buy/sell
        ShopCurrency shopCurrency = getCurrency(shopItem);
        builder.lore(Component.empty());
        builder.lore(Component.empty()
                .append(Component.text("Buy: ", NamedTextColor.GRAY))
                .append(buildPriceComponent(shopCurrency, shopItem.getBuyPrice()).append(Component.text(" ea."))));
        if (shopItem.getSellPrice() > 0) {
            builder.lore(Component.empty()
                    .append(Component.text("Sell: ", NamedTextColor.GRAY))
                    .append(buildPriceComponent(shopCurrency, shopItem.getSellPrice()).append(Component.text(" ea."))));
        }

        return builder.build();
    }

    /**
     * Builds a price display component for the given currency and amount.
     * COINS uses the {@code <coins>} MiniMessage tag (gold coin icon).
     * BARK renders the amount followed by the Tree Bark item name.
     */
    public Component buildPriceComponent(ShopCurrency currency, int amount) {
        return switch (currency) {
            case COINS -> UtilMessage.deserialize("<coins>" + NUMBER_FORMAT.format(amount) + "</coins>");
            case BARK -> {
                BaseItem barkBase = itemFactory.getItemRegistry().getItem("progression:tree_bark");
                if (barkBase == null) {
                    yield Component.text(NUMBER_FORMAT.format(amount), NamedTextColor.GREEN);
                }
//                ItemStack barkItem = itemFactory.create(barkBase).getView().getName();
//                final Key model = barkItem.getData(DataComponentTypes.ITEM_MODEL);
//                yield Component.empty()
//                        .append(Component.text(NUMBER_FORMAT.format(amount) + " ", NamedTextColor.GREEN))
//                        .append(Component.object(ObjectContents.sprite(model)));
//                final Key model = barkItem.getData(DataComponentTypes.ITEM_MODEL);
                yield Component.empty()
                        .append(Component.text(NUMBER_FORMAT.format(amount) + " ", NamedTextColor.GREEN))
                        .append(itemFactory.create(barkBase).getView().getName().color(NamedTextColor.GREEN));
            }
        };
    }
}
