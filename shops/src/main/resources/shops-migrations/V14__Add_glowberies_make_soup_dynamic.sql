INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice) VALUES ('Farming', 'GLOW_BERRIES', 'Glow Berries', 0, 48, 1, 1, 60, 30);
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'GLOW_BERRIES';
INSERT IGNORE INTO shopitems_dynamic_pricing VALUES (@shopItemId, 10, 25, 40, 50, 55, 60, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Weapons / Tools' AND ItemName = 'Mushroom Stew';
INSERT IGNORE INTO shopitems_dynamic_pricing VALUES (@shopItemId, 100, 500, 900, 600, 1000, 1400, 25000, 50000, 25000);