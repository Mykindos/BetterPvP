SELECT id INTO @diamondAxeShopItemId FROM shopitems WHERE Shopkeeper = 'Lumberjack' AND Material = 'DIAMOND_AXE';
SELECT id INTO @goldenAxeShopItemId FROM shopitems WHERE Shopkeeper = 'Lumberjack' AND Material = 'GOLDEN_AXE';
SELECT id INTO @ironAxeShopItemId FROM shopitems WHERE Shopkeeper = 'Lumberjack' AND Material = 'IRON_AXE';
SELECT id INTO @saplingShopItemId FROM shopitems WHERE Shopkeeper = 'Lumberjack' AND Material = 'MANGROVE_PROPAGULE';
INSERT IGNORE INTO shopitems_flags (shopItemId, PersistentKey, PersistentValue) VALUES (@diamondAxeShopItemId, 'SHOP_CURRENCY', 'BARK');
INSERT IGNORE INTO shopitems_flags (shopItemId, PersistentKey, PersistentValue) VALUES (@goldenAxeShopItemId, 'SHOP_CURRENCY', 'BARK');
INSERT IGNORE INTO shopitems_flags (shopItemId, PersistentKey, PersistentValue) VALUES (@ironAxeShopItemId, 'SHOP_CURRENCY', 'BARK');
INSERT IGNORE INTO shopitems_flags (shopItemId, PersistentKey, PersistentValue) VALUES (@saplingShopItemId , 'SHOP_CURRENCY', 'BARK');
