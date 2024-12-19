INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice) VALUES ('Lumberjack', 'BONE_MEAL', '<light_purple>Rebirth of Magnolia', 1, 17, 1, 1, 10, 0);
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Lumberjack' AND Material = 'BONE_MEAL';
INSERT IGNORE INTO shopitems_flags (shopItemId, PersistentKey, PersistentValue) VALUES (@shopItemId, 'SHOP_CURRENCY', 'BARK');
UPDATE shopitems SET BuyPrice = 12 WHERE Material = 'DIAMOND_AXE' AND Shopkeeper = 'Lumberjack';

