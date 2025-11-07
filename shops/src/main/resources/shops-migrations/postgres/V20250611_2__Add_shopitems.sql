-- Assassin
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'LEATHER_HELMET', 'Assassin Helmet', 0, 0, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'LEATHER_CHESTPLATE', 'Assassin Chestplate', 0, 9, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'LEATHER_LEGGINGS', 'Assassin Leggings', 0, 18, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'LEATHER_BOOTS', 'Assassin Boots', 0, 27, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Knight
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'IRON_HELMET', 'Knight Helmet', 0, 1, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'IRON_CHESTPLATE', 'Knight Chestplate', 0, 10, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'IRON_LEGGINGS', 'Knight Leggings', 0, 19, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'IRON_BOOTS', 'Knight Boots', 0, 28, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Brute
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'DIAMOND_HELMET', 'Brute Helmet', 0, 2, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'DIAMOND_CHESTPLATE', 'Brute Chestplate', 0, 11, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'DIAMOND_LEGGINGS', 'Brute Leggings', 0, 20, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'DIAMOND_BOOTS', 'Brute Boots', 0, 29, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Mage
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'GOLDEN_HELMET', 'Mage Helmet', 0, 3, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'GOLDEN_CHESTPLATE', 'Mage Chestplate', 0, 12, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'GOLDEN_LEGGINGS', 'Mage Leggings', 0, 21, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'GOLDEN_BOOTS', 'Mage Boots', 0, 30, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Ranger
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'CHAINMAIL_HELMET', 'Ranger Helmet', 0, 4, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'CHAINMAIL_CHESTPLATE', 'Ranger Chestplate', 0, 13, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'CHAINMAIL_LEGGINGS', 'Ranger Leggings', 0, 22, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'CHAINMAIL_BOOTS', 'Ranger Boots', 0, 31, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Warlock
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'NETHERITE_HELMET', 'Warlock Helmet', 0, 5, 1, 1, 5000, 1250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'NETHERITE_CHESTPLATE', 'Warlock Chestplate', 0, 14, 1, 1, 8000, 2000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'NETHERITE_LEGGINGS', 'Warlock Leggings', 0, 23, 1, 1, 7000, 1750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Armor', 'NETHERITE_BOOTS', 'Warlock Boots', 0, 32, 1, 1, 4000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Blocks
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'STONE', 'Stone', 0, 0, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'COBBLESTONE', 'Cobblestone', 0, 1, 1, 1, 20, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'DIRT', 'Dirt', 0, 3, 1, 1, 5, 2)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'OAK_LOG', 'Log', 0, 18, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'OAK_PLANKS', 'Wood', 0, 19, 1, 1, 7, 3)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'WHITE_WOOL', 'Wool', 0, 26, 1, 1, 450, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLASS', 'Glass', 0, 36, 1, 1, 100, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SAND', 'Sand', 0, 37, 1, 1, 10, 2)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SMOOTH_SANDSTONE', 'Smooth Sandstone', 2, 38, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PISTON', 'Piston', 0, 43, 1, 1, 3000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'STICKY_PISTON', 'Sticky Piston', 0, 44, 1, 1, 5000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'STONE_BRICKS', 'Stone brick', 0, 9, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLOWSTONE', 'Glowstone', 0, 10, 1, 1, 700, 350)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GRAVEL', 'Gravel', 0, 45, 1, 1, 100, 20)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SPONGE', 'Sponge', 0, 35, 1, 1, 2000, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SMOOTH_QUARTZ', 'Smooth Quartz', 0, 5, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'NETHER_BRICKS', 'Nether Brick', 0, 14, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PRISMARINE_BRICKS', 'Prismarine Brick', 1, 6, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'LAPIS_BLOCK', 'Lapis Block', 0, 53, 1, 1, 2000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'JUNGLE_LOG', 'Jungle Log', 3, 27, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SOUL_SAND', 'Soulsand', 0, 46, 1, 1, 20, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'DARK_OAK_LOG', 'Dark Oak Log', 1, 28, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'BIRCH_LOG', 'Birch Log', 2, 29, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SPRUCE_LOG', 'Spruce Log', 1, 30, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'MOSSY_COBBLESTONE', 'Mossy Cobblestone', 0, 2, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'END_STONE_BRICKS', 'End Stone Brick', 0, 31, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'POLISHED_BLACKSTONE_BRICKS', 'Blackstone Bricks', 0, 23, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PURPUR_BLOCK', 'Purpur Block', 0, 32, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'WHITE_CONCRETE', 'White Concrete', 0, 50, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Weapons / Tools
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'COBWEB', 'Throwing Web', 0, 53, 1, 1, 2000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'MAGMA_CREAM', 'Incendiary Grenade', 0, 44, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'BLACK_TERRACOTTA', 'Gravity Bomb', 0, 52, 1, 1, 2000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'POTION', 'Extinguishing Potion', 1, 50, 1, 1, 500, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'APPLE', 'Energy Apple', 0, 49, 1, 1, 500, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'ENDER_PEARL', 'Purifying Capsule', 0, 43, 1, 1, 2500, 1000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'GOLDEN_AXE', 'Fire axe', 0, 3, 1, 1, 5000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'DIAMOND_SWORD', 'Power Sword', 0, 0, 1, 1, 5000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'GOLDEN_SWORD', 'Booster Sword', 0, 9, 1, 1, 5000, 2500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'IRON_SWORD', 'Standard Sword', 0, 18, 1, 1, 2000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'BOW', 'Bow', 0, 45, 1, 1, 300, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'ARROW', 'Arrow', 0, 46, 1, 1, 12, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'DIAMOND_PICKAXE', 'Diamond Pickaxe', 0, 6, 1, 1, 3000, 750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'DIAMOND_SHOVEL', 'Diamond Shovel', 0, 7, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'DIAMOND_HOE', 'Diamond Hoe', 0, 8, 1, 1, 2000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'IRON_PICKAXE', 'Iron Pickaxe', 0, 15, 1, 1, 2000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'IRON_SHOVEL', 'Iron Shovel', 0, 16, 1, 1, 500, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'IRON_HOE', 'Iron Hoe', 0, 17, 1, 1, 1000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'DIAMOND_AXE', 'Power Axe', 0, 1, 1, 1, 6000, 3000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'GOLDEN_AXE', 'Booster Axe', 0, 10, 1, 1, 6000, 3000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'FLINT_AND_STEEL', 'Flint and Steel', 0, 39, 1, 1, 1500, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'IRON_AXE', 'Standard Axe', 0, 19, 1, 1, 3000, 750)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'NETHER_STAR', 'EMP Grenade', 0, 35, 1, 1, 5000, 2500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Resources
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'DIAMOND', 'Diamond', 0, 0, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'DIAMOND_BLOCK', 'Diamond Block', 0, 9, 1, 1, 9000, 2250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'IRON_INGOT', 'Iron Ingot', 0, 1, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'IRON_BLOCK', 'Iron Block', 0, 10, 1, 1, 9000, 2250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'GOLD_INGOT', 'Gold Ingot', 0, 2, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'GOLD_BLOCK', 'Gold Block', 0, 11, 1, 1, 9000, 2250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'EMERALD', 'Emerald', 0, 3, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'EMERALD_BLOCK', 'Emerald Block', 0, 12, 1, 1, 9000, 2250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'NETHERITE_INGOT', 'Netherite Ingot', 0, 4, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'NETHERITE_BLOCK', 'Netherite Block', 0, 13, 1, 1, 9000, 2250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'LEATHER', 'Leather', 0, 5, 1, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'TNT', 'TNT', 0, 8, 1, 1, 150000, 75000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'REDSTONE', 'Redstone', 0, 27, 1, 1, 200, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'REDSTONE_BLOCK', 'Redstone Block', 0, 28, 1, 1, 1800, 900)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'QUARTZ', 'Quartz', 0, 36, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'BONE', 'Bone', 0, 45, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'STRING', 'String', 0, 46, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'COAL', 'Coal', 0, 6, 1, 1, 300, 150)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'ENCHANTING_TABLE', 'Enchantment Table', 0, 32, 1, 1, 100000, 50000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'MUSIC_DISC_WAIT', '100,000', 0, 35, 1, 1, 100000, 100000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'MUSIC_DISC_13', '50,000', 0, 26, 1, 1, 50000, 50000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'MUSIC_DISC_PIGSTEP', '1,000,000', 0, 44, 1, 1, 1000000, 1000000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Farming
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'PUMPKIN', 'Pumpkin', 0, 0, 1, 1, 60, 30)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'COCOA_BEANS', 'Cocoa Beans', 0, 45, 1, 1, 50, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'SUGAR_CANE', 'Sugar Canes', 0, 36, 1, 1, 10, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'MELON', 'Melon', 0, 1, 1, 1, 20, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'CARROT', 'Carrot', 0, 27, 1, 1, 40, 20)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'POTATO', 'Potato', 0, 36, 1, 1, 40, 20)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'NETHER_WART', 'Nether Wart', 0, 35, 1, 1, 50, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'WHEAT', 'Wheat', 0, 3, 1, 1, 60, 30)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'BEETROOT', 'Beetroot', 0, 39, 1, 1, 60, 30)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'SWEET_BERRIES', 'Sweet Berries', 0, 30, 1, 1, 40, 20)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'HONEYCOMB', 'Honeycomb', 0, 8, 1, 1, 3334, 600)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'BEEHIVE', 'Beehive', 0, 17, 1, 1, 10000, 5000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'PUMPKIN_SEEDS', 'Pumpkin Seeds', 0, 9, 1, 1, 5, 1)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'MELON_SEEDS', 'Melon Seeds', 0, 10, 1, 1, 5, 1)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'BEETROOT_SEEDS', 'Beetroot Seeds', 0, 40, 1, 1, 5, 1)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'WHEAT_SEEDS', 'Seeds', 0, 4, 1, 1, 5, 1)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Fishing
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'FISHING_ROD', 'Fishing Rod', 0, 45, 1, 1, 500, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'DIAMOND_AXE', 'Power Axe', 0, 12, 1, 1, 16, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'GOLDEN_AXE', 'Booster Axe', 0, 13, 1, 1, 12, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'IRON_AXE', 'Standard Axe', 0, 14, 1, 1, 6, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'MANGROVE_PROPAGULE', 'Mangrove Propagule', 0, 8, 1, 1, 3, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'OAK_WOOD', 'Compacted Log', 1, 27, 1, 1, 3200, 1600)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'ANVIL', 'Anvil', 0, 33, 1, 1, 50000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'ORANGE_GLAZED_TERRACOTTA', 'Speedy Bait', 1, 46, 1, 1, 5000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

DELETE FROM shopitems WHERE item_name = 'Throwing Web';
DELETE FROM shopitems WHERE item_name = 'Incendiary Grenade';
DELETE FROM shopitems WHERE item_name = 'Gravity Bomb';
DELETE FROM shopitems WHERE item_name = 'Extinguishing Potion';
DELETE FROM shopitems WHERE item_name = 'EMP Grenade';
DELETE FROM shopitems WHERE item_name = 'Purifying Capsule';
DELETE FROM shopitems WHERE item_name = 'Flint And Steel';
DELETE FROM shopitems WHERE item_name = 'Fire Axe';
DELETE FROM shopitems WHERE item_name = 'TNT';

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'FERMENTED_SPIDER_EYE', 'Cannon', 1, 35, 1, 1, 30000, 15000)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'SHULKER_SHELL', 'Cannonball', 2, 44, 1, 1, 15000, 7500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'PUMPKIN_PIE', 'Mushroom Stew', 1, 40, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'SPIDER_EYE', 'Rabbit Stew', 2, 31, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Weapons / Tools', 'ROTTEN_FLESH', 'Suspicious Stew', 3, 22, 1, 1, 1000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems SET buy_price = 50, sell_price = 25 WHERE material ILIKE '%_LOG%';
UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building' AND material = 'SMOOTH_SANDSTONE';

UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building' AND material = 'DARK_OAK_LOG';

UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building' AND material = 'BIRCH_LOG';

UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building' AND material = 'SPRUCE_LOG';

UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building' AND material = 'JUNGLE_LOG';

UPDATE shopitems SET model_data = 0 WHERE shopkeeper = 'Building';

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'WARPED_STEM', 'Warped Stem', 0, 21, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'CRIMSON_STEM', 'Crimson Stem', 0, 22, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'GLOW_BERRIES', 'Glow Berries', 0, 48, 1, 1, 60, 30)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- move items displaced by GUI change
UPDATE shopitems
SET menu_slot = 30
WHERE shopkeeper = 'Weapons / Tools'
  AND material = 'APPLE';

UPDATE shopitems
SET menu_slot = 38
WHERE shopkeeper = 'Farming'
  AND material = 'GLOW_BERRIES';

UPDATE shopitems
SET menu_slot = 8
WHERE shopkeeper = 'Building'
  AND material = 'WHITE_CONCRETE';

UPDATE shopitems
SET menu_slot = 34
WHERE shopkeeper = 'Building'
  AND material = 'LAPIS_BLOCK';


INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES
    ('Building', 'REPEATER', 'Redstone Repeater', 0, 10, 2, 1, 2500, 1250),
    ('Building', 'COMPARATOR', 'Redstone Comparator', 0, 11, 2, 1, 3000, 1500),

    ('Building', 'IRON_TRAPDOOR', 'Iron Trapdoor', 0, 15, 2, 1, 500, 0),
    ('Building', 'IRON_DOOR', 'Iron Door', 0, 16, 2, 1, 500, 0),

    ('Building', 'REDSTONE_TORCH', 'Redstone Torch', 0, 19, 2, 1, 2000, 1000),
    ('Building', 'TARGET', 'Target', 0, 28, 2, 1, 2000, 1000),
    ('Building', 'LEVER', 'Lever', 0, 29, 2, 1, 1500, 750),
    ('Building', 'TRIPWIRE_HOOK', 'Tripwire Hook', 0, 30, 2, 1, 2000, 1000),

    ('Building', 'DAYLIGHT_DETECTOR', 'Daylight Detector', 0, 33, 2, 1, 2000, 1000),
    ('Building', 'REDSTONE_LAMP', 'Redstone Lamp', 0, 34, 2, 1, 1000, 500),

    ('Building', 'HOPPER_MINECART', 'Minecart with Hopper', 0, 37, 2, 1, 10000, 5000),
    ('Building', 'RAIL', 'Rail', 0, 38, 2, 16, 5000, 2500),
    ('Building', 'DETECTOR_RAIL', 'Detector Rail', 0, 39, 2, 6, 5000, 2500),
    ('Building', 'POWERED_RAIL', 'Powered Rail', 0, 40, 2, 6, 5000, 2500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET menu_slot = 42, menu_page = 2
WHERE shopkeeper = 'Building'
  AND material = 'PISTON';

UPDATE shopitems
SET menu_slot = 43, menu_page = 2
WHERE shopkeeper = 'Building'
  AND material = 'STICKY_PISTON';


INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES
    ('Building', 'RED_DYE', 'Red Dye', 0, 20, 3, 1, 500, 250),
    ('Building', 'BROWN_DYE', 'Brown Dye', 0, 21, 3, 1, 500, 250),
    ('Building', 'YELLOW_DYE', 'Yellow Dye', 0, 22, 3, 1, 500, 250),
    ('Building', 'GREEN_DYE', 'Green Dye', 0, 23, 3, 1, 500, 250),
    ('Building', 'BLUE_DYE', 'Blue Dye', 0, 24, 3, 1, 500, 250),
    ('Building', 'PURPLE_DYE', 'Purple Dye', 0, 30, 3, 1, 500, 250),
    ('Building', 'BLACK_DYE', 'Black Dye', 0, 31, 3, 1, 500, 250),
    ('Building', 'WHITE_DYE', 'White Dye', 0, 32, 3, 1, 500, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;


INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES
    ('Lumberjack', 'OAK_SAPLING', 'Oak Sapling', 0, 37, 1, 1, 100, 10),
    ('Lumberjack', 'SPRUCE_SAPLING', 'Spruce Sapling', 0, 38, 1, 1, 100, 10),
    ('Lumberjack', 'BIRCH_SAPLING', 'Birch Sapling', 0, 39, 1, 1, 100, 10),
    ('Lumberjack', 'JUNGLE_SAPLING', 'Jungle Sapling', 0, 40, 1, 1, 100, 10),
    ('Lumberjack', 'ACACIA_SAPLING', 'Acacia Sapling', 0, 41, 1, 1, 100, 10),
    ('Lumberjack', 'DARK_OAK_SAPLING', 'Dark Oak Sapling', 0, 42, 1, 1, 100, 10),
    ('Lumberjack', 'CHERRY_SAPLING', 'Cherry Sapling', 0, 43, 1, 1, 100, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES
    ('Resources', 'PRISMARINE_SHARD', 'Prismarine Shard', 0, 37, 1, 1, 10, 1)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Page 2 of Lumberjack
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'MOSS_BLOCK', 'Moss Block', 0, 10, 2, 1, 20, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'FLOWERING_AZALEA', 'Flowering Azalea', 0, 11, 2, 1, 500, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'AZALEA', 'Azalea', 0, 12, 2, 1, 500, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'BIG_DRIPLEAF', 'Big Dripleaf', 0, 13, 2, 1, 500, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'SMALL_DRIPLEAF', 'Small Dripleaf', 0, 14, 2, 1, 500, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'WARPED_NYLIUM', 'Warped Nylium', 0, 15, 2, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'CRIMSON_NYLIUM', 'Crimson Nylium', 0, 16, 2, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'PINK_PETALS', 'Pink Petals', 0, 19, 2, 1, 150, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'SPORE_BLOSSOM', 'Spore Blossom', 0, 20, 2, 1, 2000, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'VINE', 'Vines', 0, 21, 2, 1, 50, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'LILY_PAD', 'Lily Pad', 0, 22, 2, 1, 1500, 75)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'DECORATED_POT', 'Decorated Pot', 0, 23, 2, 1, 2000, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'TWISTING_VINES', 'Twisting Vines', 0, 24, 2, 1, 250, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'WEEPING_VINES', 'Weeping Vines', 0, 25, 2, 1, 250, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'HORN_CORAL', 'Horn Coral', 0, 28, 2, 1, 750, 200)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'BRAIN_CORAL', 'Brain Coral', 0, 29, 2, 1, 750, 200)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'FIRE_CORAL', 'Fire Coral', 0, 30, 2, 1, 750, 200)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'BUBBLE_CORAL', 'Bubble Coral', 0, 31, 2, 1, 750, 200)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'TUBE_CORAL', 'Tube Coral', 0, 32, 2, 1, 750, 200)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'WARPED_ROOTS', 'Warped Roots', 0, 33, 2, 1, 100, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'CRIMSON_ROOTS', 'Crimson Roots', 0, 34, 2, 1, 100, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'PEONY', 'Peony', 0, 37, 2, 1, 750, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'ROSE_BUSH', 'Rose Bush', 0, 38, 2, 1, 750, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'LILAC', 'Lilac', 0, 39, 2, 1, 750, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'TORCHFLOWER', 'Torchflower', 0, 40, 2, 1, 1000, 250)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'SEAGRASS', 'Seagrass', 0, 41, 2, 1, 50, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'SEA_PICKLE', 'Sea Pickle', 0, 42, 2, 1, 2000, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Lumberjack', 'KELP', 'Kelp', 0, 43, 2, 1, 50, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Updated Page 1 of Building

DELETE FROM shopitems WHERE shopkeeper='Building' AND menu_page=1 AND menu_slot=6;
DELETE FROM shopitems WHERE shopkeeper='Building' AND menu_page=1 AND menu_slot=32;
DELETE FROM shopitems WHERE shopkeeper='Building' AND menu_page=1 AND menu_slot=16;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GRASS_BLOCK', 'Grass Block', 0, 4, 1, 1, 20, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'WHITE_WOOL', 'White Wool', 0, 7, 1, 1, 450, 50)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET material = 'PODZOL', buy_price = 20, sell_price = 5, item_name = 'Podzol'
WHERE shopkeeper = 'Building' AND menu_slot = 5 AND menu_page = 1;

UPDATE shopitems
SET material = 'POLISHED_BLACKSTONE_BRICKS', buy_price = 40, sell_price = 10, item_name = 'Polished Blackstone Bricks'
WHERE shopkeeper = 'Building' AND menu_slot = 10 AND menu_page = 1;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'BLACKSTONE', 'Blackstone', 0, 11, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'MUD', 'Mud', 0, 12, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'CALCITE', 'Calcite', 0, 13, 1, 1, 30, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems SET menu_slot = 16 WHERE shopkeeper = 'Building' AND material = 'SPONGE';

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'LAPIS_BLOCK', 'Water Block', 0, 17, 1, 1, 2000, 500)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET material = 'DEEPSLATE', buy_price = 40, sell_price = 10, item_name = 'Deepslate'
WHERE shopkeeper = 'Building' AND menu_slot = 18 AND menu_page = 1;

UPDATE shopitems
SET material = 'COBBLED_DEEPSLATE', buy_price = 40, sell_price = 10, item_name = 'Cobbled Deepslate'
WHERE shopkeeper = 'Building' AND menu_slot = 19 AND menu_page = 1;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'BASALT', 'Basalt', 0, 20, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET material = 'MUD_BRICKS', buy_price = 40, sell_price = 10, item_name = 'Mud Bricks'
WHERE shopkeeper = 'Building' AND menu_slot = 21 AND menu_page = 1;

UPDATE shopitems
SET material = 'END_STONE_BRICKS', buy_price = 40, sell_price = 10, item_name = 'End Stone Bricks'
WHERE shopkeeper = 'Building' AND menu_slot = 22 AND menu_page = 1;

UPDATE shopitems
SET material = 'PURPUR_BLOCK', buy_price = 40, sell_price = 10, item_name = 'Purpur Block'
WHERE shopkeeper = 'Building' AND menu_slot = 23 AND menu_page = 1;

UPDATE shopitems
SET material = 'SNOW_BLOCK', buy_price = 20, sell_price = 2, item_name = 'Snow Block'
WHERE shopkeeper = 'Building' AND menu_slot = 26 AND menu_page = 1;

UPDATE shopitems
SET material = 'BAMBOO_BLOCK', buy_price = 50, sell_price = 25, item_name = 'Block of Bamboo'
WHERE shopkeeper = 'Building' AND menu_slot = 31 AND menu_page = 1;

UPDATE shopitems
SET material = 'SMOOTH_QUARTZ', buy_price = 40, sell_price = 10, item_name = 'Smooth Quartz'
WHERE shopkeeper = 'Building' AND menu_slot = 34 AND menu_page = 1;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLOWSTONE', 'Glowstone', 0, 35, 1, 1, 700, 350)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET material = 'OAK_LOG', buy_price = 50, sell_price = 25, item_name = 'Oak Log'
WHERE shopkeeper = 'Building' AND menu_slot = 36 AND menu_page = 1;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'ACACIA_LOG', 'Acacia Log', 0, 39, 1, 1, 50, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems
SET material = 'WARPED_STEM', buy_price = 50, sell_price = 25, item_name = 'Warped Stem'
WHERE shopkeeper = 'Building' AND menu_slot = 38 AND menu_page = 1;

UPDATE shopitems
SET material = 'CRIMSON_STEM', buy_price = 50, sell_price = 25, item_name = 'Crimson Stem'
WHERE shopkeeper = 'Building' AND menu_slot = 37 AND menu_page = 1;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'CHERRY_LOG', 'Cherry Log', 0, 40, 1, 1, 50, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PRISMARINE_BRICKS', 'Prismarine Bricks', 0, 42, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PRISMARINE', 'Prismarine', 0, 43, 1, 1, 20, 5)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SEA_LANTERN', 'Sea Lantern', 0, 44, 1, 1, 700, 350)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'CLAY', 'Clay', 0, 47, 1, 1, 50, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SMOOTH_SANDSTONE', 'Smooth Sandstone', 0, 51, 1, 1, 40, 10)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SAND', 'Sand', 0, 52, 1, 1, 10, 2)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLASS', 'Glass', 0, 41, 1, 1, 100, 2)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Page 4 of Building
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLOW_INK_SAC', 'Glow Ink', 0, 24, 4, 1, 1000, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'END_ROD', 'End Rod', 0, 23, 4, 1, 1000, 100)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'GLOW_LICHEN', 'Glow Lichen', 0, 21, 4, 1, 100, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SHROOMLIGHT', 'Shroomlight', 0, 22, 4, 1, 700, 150)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'OCHRE_FROGLIGHT', 'Ochre Froglight', 0, 31, 4, 1, 700, 150)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'VERDANT_FROGLIGHT', 'Verdant Froglight', 0, 30, 4, 1, 700, 150)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'PEARLESCENT_FROGLIGHT', 'Pearlescent Froglight', 0, 32, 4, 1, 700, 150)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Building', 'SCULK_VEIN', 'Sculk Vein', 0, 20, 4, 1, 100, 15)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Add Copper Ores
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'COPPER_INGOT', 'Copper', 0, 7, 1, 1, 100, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Fix Dye Shop
UPDATE shopitems
SET sell_price = 0
WHERE shopkeeper = 'Building'
  AND material ILIKE '%_DYE';

UPDATE shopitems
SET buy_price = 20, sell_price = 10
WHERE shopkeeper = 'Building'
  AND material = 'LEVER';

UPDATE shopitems SET buy_price = 200, sell_price = 100 WHERE material = 'REDSTONE_TORCH';

UPDATE shopitems SET sell_price = 0 WHERE shopkeeper = 'Building' AND menu_page = 2;

INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Farming', 'BEE_SPAWN_EGG', 'Bee Spawn Egg', 0, 7, 1, 1, 50000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'STONECUTTER', '<gray>Salvager', 0, 41, 1, 1, 25000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

INSERT INTO shopitems
(shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Resources', 'GRINDSTONE', '<gray>Resource Converter', 0, 42, 1, 1, 25000, 0)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

UPDATE shopitems SET sell_price = 50 WHERE shopkeeper = 'Resources' AND material = 'STRING';

-- Trout (Slot 0, ModelData 1)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Trout', 1, 0, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Salmon (Slot 1, ModelData 2)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Salmon', 2, 1, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Bluegill (Slot 2, ModelData 3)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Bluegill', 3, 2, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Gar (Slot 3, ModelData 4)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Gar', 4, 3, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Carp (Slot 4, ModelData 5)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Carp', 5, 4, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Catfish (Slot 5, ModelData 6)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Catfish', 6, 5, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Cod (Slot 6, ModelData 7)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Cod', 7, 6, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Drum (Slot 7, ModelData 8)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Drum', 8, 7, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Sablefish (Slot 8, ModelData 9)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Sablefish', 9, 8, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Kingfish (Slot 9, ModelData 10)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Kingfish', 10, 9, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Cobia (Slot 10, ModelData 11)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Cobia', 11, 10, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Sea Bass (Slot 11, ModelData 12)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Sea Bass', 12, 11, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Tuna (Slot 12, ModelData 13)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Tuna', 13, 12, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Swordfish (Slot 13, ModelData 14)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Swordfish', 14, 13, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Marlin (Slot 14, ModelData 15)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Marlin', 15, 14, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Grouper (Slot 15, ModelData 16)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Grouper', 16, 15, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Sturgeon (Slot 16, ModelData 17)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Sturgeon', 17, 16, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;

-- Sunfish (Slot 17, ModelData 18)
INSERT INTO shopitems (shopkeeper, material, item_name, model_data, menu_slot, menu_page, amount, buy_price, sell_price)
VALUES ('Fisherman', 'COD', 'Sunfish', 18, 17, 1, 1, 40, 25)
ON CONFLICT (shopkeeper, material, item_name) DO NOTHING;
