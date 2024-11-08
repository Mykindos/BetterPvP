DELETE FROM shopitems WHERE ItemName = 'Compacted Log';
DELETE FROM shopitems WHERE ItemName = 'Anvil';
DELETE FROM shopitems WHERE ItemName = 'Speedy Bait';

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, BuyPrice, SellPrice) VALUES ('Lumberjack', 'OAK_WOOD', 'Compacted Log', 1, 27, 1, 2000, 960);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, BuyPrice, SellPrice) VALUES ('Resources', 'ANVIL', 'Anvil', 0, 33, 1, 50000, 0);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, BuyPrice, SellPrice) VALUES ('Fisherman', 'ORANGE_GLAZED_TERRACOTTA', 'Speedy Bait', 1, 0, 1, 5000, 0);