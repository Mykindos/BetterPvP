
-- Add test item
INSERT IGNORE INTO shopitems (Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice) VALUES ('Building', 'REDSTONE', 'Redstone Dust', 0, 0, 2, 1, 40, 10);


-- move items displaced by GUI change
UPDATE shopitems
    SET MenuSlot = 30
    WHERE Shopkeeper = 'Weapons / Tools'
    AND Material = 'APPLE';

UPDATE shopitems
    SET MenuSlot = 49
    WHERE Shopkeeper = 'Resources'
    AND Material = 'MUSIC_DISC_13';

UPDATE shopitems
    SET MenuSlot = 50
    WHERE Shopkeeper = 'Resources'
    AND Material = 'MUSIC_DISC_WAIT';

UPDATE shopitems
    SET MenuSlot = 38
    WHERE Shopkeeper = 'Farming'
    AND Material = 'GLOW_BERRIES';

UPDATE shopitems
    SET MenuSlot = 8
    WHERE Shopkeeper = 'Building'
    AND Material = 'WHITE_CONCRETE';

UPDATE shopitems
    SET MenuSlot = 34
    WHERE Shopkeeper = 'Building'
    AND Material = 'LAPIS_BLOCK';

