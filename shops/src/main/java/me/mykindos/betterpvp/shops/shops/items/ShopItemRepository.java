package me.mykindos.betterpvp.shops.shops.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.shops.shops.items.data.PolynomialData;
import org.bukkit.Material;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.mykindos.betterpvp.shops.database.jooq.Tables.SHOPITEMS;
import static me.mykindos.betterpvp.shops.database.jooq.Tables.SHOPITEMS_DYNAMIC_PRICING;
import static me.mykindos.betterpvp.shops.database.jooq.Tables.SHOPITEMS_FLAGS;

@Singleton
@CustomLog
public class ShopItemRepository {

    private final Database database;

    @Inject
    public ShopItemRepository(Database database) {
        this.database = database;
    }

    public Map<String, List<IShopItem>> getAllShopItems() {
        var shopItems = new HashMap<String, List<IShopItem>>();

        try {
            database.getAsyncDslContext().executeAsync(ctx -> {
                var shopItemRecords = ctx.selectFrom(SHOPITEMS).fetch();

                for (var record : shopItemRecords) {
                    int id = record.get(SHOPITEMS.ID);
                    String shopKeeper = record.get(SHOPITEMS.SHOPKEEPER);
                    Material material = Material.valueOf(record.get(SHOPITEMS.MATERIAL));
                    String itemName = record.get(SHOPITEMS.ITEM_NAME);
                    int modelData = record.get(SHOPITEMS.MODEL_DATA);
                    int menuSlot = record.get(SHOPITEMS.MENU_SLOT);
                    int menuPage = record.get(SHOPITEMS.MENU_PAGE);
                    int amount = record.get(SHOPITEMS.AMOUNT);
                    int buyPrice = record.get(SHOPITEMS.BUY_PRICE);
                    int sellPrice = record.get(SHOPITEMS.SELL_PRICE);

                    if (!shopItems.containsKey(shopKeeper)) {
                        shopItems.put(shopKeeper, new ArrayList<>());
                    }

                    ShopItem shopItem;

                    // Fetch dynamic pricing
                    var dynamicPricingRecord = ctx.selectFrom(SHOPITEMS_DYNAMIC_PRICING)
                            .where(SHOPITEMS_DYNAMIC_PRICING.SHOP_ITEM_ID.eq(id))
                            .and(SHOPITEMS_DYNAMIC_PRICING.REALM.eq(Core.getCurrentRealm().getServer().getId()))
                            .fetchOne();

                    if (dynamicPricingRecord != null) {
                        int minSellPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.MIN_SELL_PRICE);
                        int baseSellPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.BASE_SELL_PRICE);
                        int maxSellPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.MAX_SELL_PRICE);
                        int minBuyPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.MIN_BUY_PRICE);
                        int baseBuyPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.BASE_BUY_PRICE);
                        int maxBuyPrice = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.MAX_BUY_PRICE);
                        int baseStock = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.BASE_STOCK);
                        int maxStock = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.MAX_STOCK);
                        int currentStock = dynamicPricingRecord.get(SHOPITEMS_DYNAMIC_PRICING.CURRENT_STOCK);

                        PolynomialData polynomialData = new PolynomialData(
                                minBuyPrice, baseBuyPrice, maxBuyPrice,
                                minSellPrice, baseSellPrice, maxSellPrice,
                                maxStock, baseStock, currentStock
                        );
                        shopItem = new DynamicShopItem(id, shopKeeper, itemName, material, modelData, menuSlot, menuPage, amount, polynomialData);
                    } else {
                        shopItem = new NormalShopItem(id, shopKeeper, itemName, material, modelData, menuSlot, menuPage, amount, buyPrice, sellPrice);
                    }

                    // Fetch item flags
                    var itemFlagRecords = ctx.selectFrom(SHOPITEMS_FLAGS)
                            .where(SHOPITEMS_FLAGS.SHOP_ITEM_ID.eq(id))
                            .fetch();

                    for (var flagRecord : itemFlagRecords) {
                        String key = flagRecord.get(SHOPITEMS_FLAGS.PERSISTENT_KEY);
                        String value = flagRecord.get(SHOPITEMS_FLAGS.PERSISTENT_VALUE);
                        shopItem.getItemFlags().put(key, value);
                    }

                    shopItems.get(shopKeeper).add(shopItem);
                }

                return shopItems;
            }).join();

        } catch (Exception ex) {
            log.error("Failed to load shop items", ex).submit();
        }

        return shopItems;
    }

    public void updateStock(List<DynamicShopItem> dynamicShopItems) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            List<Query> queries = new ArrayList<>();

            for (DynamicShopItem dynamicShopItem : dynamicShopItems) {
                queries.add(ctx.update(SHOPITEMS_DYNAMIC_PRICING)
                        .set(SHOPITEMS_DYNAMIC_PRICING.CURRENT_STOCK, dynamicShopItem.getCurrentStock())
                        .where(SHOPITEMS_DYNAMIC_PRICING.SHOP_ITEM_ID.eq(dynamicShopItem.getId()))
                        .and(SHOPITEMS_DYNAMIC_PRICING.REALM.eq(Core.getCurrentRealm().getServer().getId())));
            }

            ctx.batch(queries).execute();
        });
    }

    public void copyTemplatedDynamicPrices() {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(SHOPITEMS_DYNAMIC_PRICING)
                    .columns(
                            SHOPITEMS_DYNAMIC_PRICING.SHOP_ITEM_ID,
                            SHOPITEMS_DYNAMIC_PRICING.REALM,
                            SHOPITEMS_DYNAMIC_PRICING.MIN_SELL_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.BASE_SELL_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.MAX_SELL_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.MIN_BUY_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.BASE_BUY_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.MAX_BUY_PRICE,
                            SHOPITEMS_DYNAMIC_PRICING.BASE_STOCK,
                            SHOPITEMS_DYNAMIC_PRICING.MAX_STOCK,
                            SHOPITEMS_DYNAMIC_PRICING.CURRENT_STOCK
                    )
                    .select(ctx.select(
                                            SHOPITEMS_DYNAMIC_PRICING.SHOP_ITEM_ID,
                                            DSL.val(Core.getCurrentRealm().getServer().getId()),
                                            SHOPITEMS_DYNAMIC_PRICING.MIN_SELL_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.BASE_SELL_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.MAX_SELL_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.MIN_BUY_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.BASE_BUY_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.MAX_BUY_PRICE,
                                            SHOPITEMS_DYNAMIC_PRICING.BASE_STOCK,
                                            SHOPITEMS_DYNAMIC_PRICING.MAX_STOCK,
                                            SHOPITEMS_DYNAMIC_PRICING.CURRENT_STOCK
                                    )
                                    .from(SHOPITEMS_DYNAMIC_PRICING)
                                    .where(SHOPITEMS_DYNAMIC_PRICING.REALM.eq(0))
                    )
                    .onConflict()
                    .doNothing()
                    .execute();
        }).join();
    }

}
