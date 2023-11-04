package me.mykindos.betterpvp.shops.shops.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.shops.shops.items.data.PolynomialData;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
@Slf4j
public class ShopItemRepository {

    @Inject
    @Config(path = "shops.database.prefix", defaultValue = "shops_")
    private String databasePrefix;

    private final Database database;

    @Inject
    public ShopItemRepository(Database database) {
        this.database = database;
    }

    public HashMap<String, List<IShopItem>> getAllShopItems() {
        var shopItems = new HashMap<String, List<IShopItem>>();

        String query = "SELECT * FROM " + databasePrefix + "shopitems";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                int id = result.getInt(1);
                String shopKeeper = result.getString(2);
                Material material = Material.valueOf(result.getString(3));
                String itemName = result.getString(4);
                int data = result.getInt(5);
                int menuSlot = result.getInt(6);
                int menuPage = result.getInt(7);
                int amount = result.getInt(8);
                int buyPrice = result.getInt(9);
                int sellPrice = result.getInt(10);

                if (!shopItems.containsKey(shopKeeper)) {
                    shopItems.put(shopKeeper, new ArrayList<>());
                }

                ShopItem shopItem;

                String dynamicPricingQuery = "SELECT * FROM " + databasePrefix + "shopitems_dynamic_pricing WHERE shopItemId = ?";
                CachedRowSet dynamicPricingResult = database.executeQuery(new Statement(dynamicPricingQuery, new IntegerStatementValue(id)));
                if (dynamicPricingResult.next()) {
                    int minSellPrice = dynamicPricingResult.getInt(2);
                    int baseSellPrice = dynamicPricingResult.getInt(3);
                    int maxSellPrice = dynamicPricingResult.getInt(4);
                    int minBuyPrice = dynamicPricingResult.getInt(5);
                    int baseBuyPrice = dynamicPricingResult.getInt(6);
                    int maxBuyPrice = dynamicPricingResult.getInt(7);
                    int baseStock = dynamicPricingResult.getInt(8);
                    int maxStock = dynamicPricingResult.getInt(9);
                    int currentStock = dynamicPricingResult.getInt(10);

                    PolynomialData polynomialData = new PolynomialData(minBuyPrice, baseBuyPrice, maxBuyPrice, minSellPrice, baseSellPrice, maxSellPrice, maxStock, baseStock, currentStock);
                    shopItem = new DynamicShopItem(id, shopKeeper, itemName, material, (byte) data, menuSlot, menuPage, amount, polynomialData);
                } else {
                    shopItem = new NormalShopItem(id, shopKeeper, itemName, material, (byte) data, menuSlot, menuPage, amount, buyPrice, sellPrice);
                }

                String itemFlagQuery = "SELECT * FROM " + databasePrefix + "shopitems_flags WHERE shopItemId = ?";
                CachedRowSet itemFlagResult = database.executeQuery(new Statement(itemFlagQuery, new IntegerStatementValue(id)));
                while (itemFlagResult.next()) {
                    String key = itemFlagResult.getString(3);
                    String value = itemFlagResult.getString(4);
                    shopItem.getItemFlags().put(key, value);
                }

                shopItems.get(shopKeeper).add(shopItem);

            }
        } catch (Exception ex) {
            log.error("Failed to load shop items", ex);
        }

        return shopItems;
    }

    public void updateStock(List<DynamicShopItem> dynamicShopItems) {
        List<Statement> updateQueries = new ArrayList<>();
        String query = "UPDATE " + databasePrefix + "shopitems_dynamic_pricing SET currentStock = ? WHERE shopItemId = ?";
        dynamicShopItems.forEach(dynamicShopItem -> {
            updateQueries.add(new Statement(query, new IntegerStatementValue(dynamicShopItem.getCurrentStock()), new IntegerStatementValue(dynamicShopItem.getId())));
        });

        database.executeBatch(updateQueries, true);
    }

}
