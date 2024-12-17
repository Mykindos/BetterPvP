
INSERT IGNORE INTO shopitems
(Shopkeeper, Material, ItemName, ModelData, MenuSlot, MenuPage, Amount, BuyPrice, SellPrice)
VALUES
('Building', 'REPEATER', 'Redstone Repeater', 0, 10, 2, 1, 2500, 1250),
('Building', 'COMPARATOR', 'Redstone Comparator', 0, 11, 2, 1, 3000, 1500),

('Building', 'IRON_TRAPDOOR', 'Iron Trapdoor', 0, 15, 2, 1, 3000, 1500),
('Building', 'IRON_DOOR', 'Iron Door', 0, 16, 2, 1, 3000, 1500),

('Building', 'REDSTONE_TORCH', 'Redstone Torch', 0, 19, 2, 1, 2000, 1000),
('Building', 'TARGET', 'Target', 0, 28, 2, 1, 2000, 1000),
('Building', 'LEVER', 'Lever', 0, 29, 2, 1, 1500, 750),
('Building', 'TRIPWIRE_HOOK', 'Tripwire Hook', 0, 30, 2, 1, 2000, 1000),

('Building', 'DAYLIGHT_DETECTOR', 'Daylight Detector', 0, 33, 2, 1, 2000, 1000),
('Building', 'REDSTONE_LAMP', 'Redstone Lamp', 0, 34, 2, 1, 1000, 500),

('Building', 'HOPPER_MINECART', 'Minecart with Hopper', 0, 37, 2, 1, 10000, 5000),
('Building', 'RAIL', 'Rail', 0, 38, 2, 1, 300, 150),
('Building', 'DETECTOR_RAIL', 'Detector Rail', 0, 39, 2, 1, 900, 450),
('Building', 'POWERED_RAIL', 'Powered Rail', 0, 40, 2, 1, 900, 450)
;

UPDATE shopitems
    SET MenuSlot = 42, MenuPage = 2
    WHERE Shopkeeper = 'Building'
    AND Material = 'PISTON';

UPDATE shopitems
    SET MenuSlot = 43, MenuPage = 2
    WHERE Shopkeeper = 'Building'
    AND Material = 'STICKY_PISTON';

