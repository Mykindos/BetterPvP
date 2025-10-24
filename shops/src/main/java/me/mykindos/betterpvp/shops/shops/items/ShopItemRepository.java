package me.mykindos.betterpvp.shops.shops.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.shops.shops.items.data.PolynomialData;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
@CustomLog
public class ShopItemRepository {

    private final Database database;

    @Inject
    public ShopItemRepository(Database database) {
        this.database = database;
    }

    public HashMap<String, List<IShopItem>> getAllShopItems() {
        var shopItems = new HashMap<String, List<IShopItem>>();

        String query = "SELECT * FROM shopitems";

        try (CachedRowSet result = database.executeQuery(new Statement(query), TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                int id = result.getInt(1);
                String shopKeeper = result.getString(2);
                Material material = Material.valueOf(result.getString(3));
                String itemName = result.getString(4);
                int modelData = result.getInt(5);
                int menuSlot = result.getInt(6);
                int menuPage = result.getInt(7);
                int amount = result.getInt(8);
                int buyPrice = result.getInt(9);
                int sellPrice = result.getInt(10);

                if (!shopItems.containsKey(shopKeeper)) {
                    shopItems.put(shopKeeper, new ArrayList<>());
                }

                ShopItem shopItem;

                String dynamicPricingQuery = "SELECT * FROM shopitems_dynamic_pricing WHERE shopItemId = ? AND Server = ? AND Season = ?";
                Statement statement = new Statement(dynamicPricingQuery,
                        new IntegerStatementValue(id),
                        new IntegerStatementValue(Core.getCurrentServer()),
                        new IntegerStatementValue(Core.getCurrentSeason()));
                try (CachedRowSet dynamicPricingResult = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
                    if (dynamicPricingResult.next()) {
                        int minSellPrice = dynamicPricingResult.getInt(4);
                        int baseSellPrice = dynamicPricingResult.getInt(5);
                        int maxSellPrice = dynamicPricingResult.getInt(6);
                        int minBuyPrice = dynamicPricingResult.getInt(7);
                        int baseBuyPrice = dynamicPricingResult.getInt(8);
                        int maxBuyPrice = dynamicPricingResult.getInt(9);
                        int baseStock = dynamicPricingResult.getInt(10);
                        int maxStock = dynamicPricingResult.getInt(11);
                        int currentStock = dynamicPricingResult.getInt(12);

                        PolynomialData polynomialData = new PolynomialData(minBuyPrice, baseBuyPrice, maxBuyPrice, minSellPrice, baseSellPrice, maxSellPrice, maxStock, baseStock, currentStock);
                        shopItem = new DynamicShopItem(id, shopKeeper, itemName, material, modelData, menuSlot, menuPage, amount, polynomialData);
                    } else {
                        shopItem = new NormalShopItem(id, shopKeeper, itemName, material, modelData, menuSlot, menuPage, amount, buyPrice, sellPrice);
                    }
                } catch (SQLException ex) {
                    log.error("Failed to load dynamic pricing for shop item {}", id, ex).submit();
                    continue;
                }

                String itemFlagQuery = "SELECT * FROM shopitems_flags WHERE shopItemId = ?";
                try (CachedRowSet itemFlagResult = database.executeQuery(new Statement(itemFlagQuery, new IntegerStatementValue(id)), TargetDatabase.GLOBAL).join()) {
                    while (itemFlagResult.next()) {
                        String key = itemFlagResult.getString(3);
                        String value = itemFlagResult.getString(4);
                        shopItem.getItemFlags().put(key, value);
                    }
                } catch (SQLException ex) {
                    log.error("Failed to load item flags for shop item {}", id, ex).submit();
                    continue;
                }

                shopItems.get(shopKeeper).add(shopItem);

            }
        } catch (Exception ex) {
            log.error("Failed to load shop items", ex).submit();
        }

        return shopItems;
    }

    public void copyTemplatedDynamicPrices() {
        String query = """
                INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice,
                MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
                SELECT shopItemId, ?, ?, MinSellPrice, BaseSellPrice, MaxSellPrice,
                       MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock
                FROM shopitems_dynamic_pricing
                WHERE Server = 0 AND Season = 0
                """;
        database.executeUpdate(new Statement(query, IntegerStatementValue.of(Core.getCurrentServer()), IntegerStatementValue.of(Core.getCurrentSeason())), TargetDatabase.GLOBAL).join();
    }

    public void updateStock(List<DynamicShopItem> dynamicShopItems) {
        List<Statement> updateQueries = new ArrayList<>();
        String query = "UPDATE shopitems_dynamic_pricing SET currentStock = ? WHERE shopItemId = ? AND Server = ? AND Season = ?";
        dynamicShopItems.forEach(dynamicShopItem -> {
            updateQueries.add(new Statement(query,
                    new IntegerStatementValue(dynamicShopItem.getCurrentStock()),
                    new IntegerStatementValue(dynamicShopItem.getId()),
                    new IntegerStatementValue(Core.getCurrentServer()),
                    new IntegerStatementValue(Core.getCurrentSeason())));
        });

        database.executeBatch(updateQueries, TargetDatabase.GLOBAL);
    }

}
