UPDATE shopitems SET ModelData = 0 WHERE Shopkeeper = 'Building';

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice) VALUES ('Building', 'WARPED_STEM', 'Warped Stem', 0, 21, 1, 1, 30, 15);
INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice) VALUES ('Building', 'CRIMSON_STEM', 'Crimson Stem', 0, 22, 1, 1, 30, 15);