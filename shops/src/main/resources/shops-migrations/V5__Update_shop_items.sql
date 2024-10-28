DELETE FROM shopitems WHERE ItemName = 'Throwing Web';
DELETE FROM shopitems WHERE ItemName = 'Incendiary Grenade';
DELETE FROM shopitems WHERE ItemName = 'Gravity Bomb';
DELETE FROM shopitems WHERE ItemName = 'Extinguishing Potion';
DELETE FROM shopitems WHERE ItemName = 'EMP Grenade';
DELETE FROM shopitems WHERE ItemName = 'Purifying Capsule';
DELETE FROM shopitems WHERE ItemName = 'Flint And Steel';
DELETE FROM shopitems WHERE ItemName = 'Fire Axe';
DELETE FROM shopitems WHERE ItemName = 'TNT';

UPDATE shopitems SET BuyPrice = 5000, SellPrice = 1500 WHERE ItemName = 'Crossbow';

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES ('Weapons / Tools', 'FERMENTED_SPIDER_EYE', 'Cannon', 1, 53, 1, 1, 30000, 15000);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES ('Weapons / Tools', 'SHULKER_SHELL', 'Cannonball', 2, 44, 1, 1, 15000, 7500);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES ('Weapons / Tools', 'PUMPKIN_PIE', 'Mushroom Stew', 1, 40, 1, 1, 1000, 500);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES ('Weapons / Tools', 'SPIDER_EYE', 'Rabbit Stew', 2, 31, 1, 1, 1000, 500);

INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES ('Weapons / Tools', 'ROTTEN_FLESH', 'Suspicious Stew', 3, 22, 1, 1, 1000, 500);