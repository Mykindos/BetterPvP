SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'PUMPKIN';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 20, 30, 60, 70, 80, 100000, 200000, 100000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'COCOA_BEANS';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 5, 25, 30, 40, 50, 60, 25000, 50000, 25000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'SUGAR_CANE';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 20, 25, 30, 35, 40, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'MELON';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 30, 40, 65, 75, 85, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'CARROT';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 40, 45, 50, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'POTATO';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 40, 45, 50, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'NETHER_WART';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 45, 55, 65, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'WHEAT';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 50, 55, 60, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'BEETROOT';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 50, 55, 60, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'SWEET_BERRIES';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 7, 23, 35, 40, 45, 50, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'HONEYCOMB';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 400, 750, 1000, 3333, 3333, 3600, 5000, 10000, 5000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'BEEHIVE';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 50, 55, 60, 50000, 100000, 50000);

UPDATE shopitems_dynamic_pricing SET MinSellPrice = 50, BaseSellPrice = 500, MaxSellPrice = 1000, MinBuyPrice = 3333,
                                     BaseBuyPrice = 3333, MaxBuyPrice= 3333, MaxStock = 15000, CurrentStock = 7500
WHERE shopItemId = (SELECT id FROM shopitems WHERE Material = 'HONEYCOMB');

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Farming' AND Material = 'GLOW_BERRIES';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 40, 50, 55, 60, 50000, 100000, 50000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Weapons / Tools' AND ItemName = 'Mushroom Stew';
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 100, 500, 900, 600, 1000, 1400, 25000, 50000, 25000);

SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Weapons / Tools' AND ItemName = 'Mushroom Stew';
UPDATE shopitems_dynamic_pricing SET BaseStock = 500, MaxStock = 1000, CurrentStock = 500 WHERE shopItemId = @shopItemId;


-- Trout (custom_model_data: 1)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 1;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Salmon (custom_model_data: 2)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 2;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Bluegill (custom_model_data: 3)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 3;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Gar (custom_model_data: 4)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 4;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Carp (custom_model_data: 5)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 5;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Catfish (custom_model_data: 6)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 6;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Cod (custom_model_data: 7)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 7;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Drum (custom_model_data: 8)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 8;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Sablefish (custom_model_data: 9)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 9;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Kingfish (custom_model_data: 10)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 10;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Cobia (custom_model_data: 11)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 11;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Sea Bass (custom_model_data: 12)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 12;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Tuna (custom_model_data: 13)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 13;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Swordfish (custom_model_data: 14)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 14;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Marlin (custom_model_data: 15)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 15;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Grouper (custom_model_data: 16)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 16;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Sturgeon (custom_model_data: 17)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 17;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);

-- Sunfish (custom_model_data: 18)
SELECT id INTO @shopItemId FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = 'COD' AND ModelData = 18;
INSERT IGNORE INTO shopitems_dynamic_pricing (shopItemId, Server, Season, MinSellPrice, BaseSellPrice, MaxSellPrice, MinBuyPrice, BaseBuyPrice, MaxBuyPrice, BaseStock, MaxStock, CurrentStock)
VALUES (@shopItemId, "TEMPLATE", "TEMPLATE", 10, 25, 35, 35, 40, 45, 50000, 200000, 50000);