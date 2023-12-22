-- Class armours
INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_HELMET', 'champions', 'assassin_helmet', '<yellow>Assassin Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_CHESTPLATE', 'champions', 'assassin_vest', '<yellow>Assassin Vest', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_LEGGINGS', 'champions', 'assassin_leggings', '<yellow>Assassin Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_BOOTS', 'champions', 'assassin_boots', '<yellow>Assassin Boots', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_HELMET', 'champions', 'knight_helmet', '<yellow>Knight Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_CHESTPLATE', 'champions', 'knight_vest', '<yellow>Knight Vest', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_LEGGINGS', 'champions', 'knight_leggings', '<yellow>Knight Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_BOOTS', 'champions', 'knight_boots', '<yellow>Knight Boots', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_HELMET', 'champions', 'warlock_helmet', '<yellow>Warlock Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_CHESTPLATE', 'champions', 'warlock_chestplate', '<yellow>Warlock Chestplate', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_LEGGINGS', 'champions', 'warlock_leggings', '<yellow>Warlock Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_BOOTS', 'champions', 'warlock_boots', '<yellow>Warlock Boots', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_HELMET', 'champions', 'ranger_helmet', '<yellow>Ranger Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_CHESTPLATE', 'champions', 'ranger_vest', '<yellow>Ranger Vest', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_LEGGINGS', 'champions', 'ranger_leggings', '<yellow>Ranger Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_BOOTS', 'champions', 'ranger_boots', '<yellow>Ranger Boots', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_HELMET', 'champions', 'mage_helmet', '<yellow>Mage Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_CHESTPLATE', 'champions', 'mage_vest', '<yellow>Mage Vest', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_LEGGINGS', 'champions', 'mage_leggings', '<yellow>Mage Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_BOOTS', 'champions', 'mage_boots', '<yellow>Mage Boots', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_HELMET', 'champions', 'brute_helmet', '<yellow>Brute Helmet', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_CHESTPLATE', 'champions', 'brute_vest', '<yellow>Brute Vest', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_LEGGINGS', 'champions', 'brute_leggings', '<yellow>Brute Leggings', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_BOOTS', 'champions', 'brute_boots', '<yellow>Brute Boots', 500, 0, 0, 0);

-- Weapons
INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_AXE', 'champions', 'booster_axe', '<yellow>Booster Axe', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_AXE' AND Namespace = 'champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_AXE' AND Namespace = 'champions'), 1, '<gray>+1 Level to Axe Skills');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_SWORD', 'champions', 'booster_sword', '<yellow>Booster Sword', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_SWORD' AND Namespace = 'champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_SWORD' AND Namespace = 'champions'), 1, '<gray>+1 Level to Sword Skills');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_SWORD', 'champions', 'standard_sword', '<yellow>Standard Sword', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'IRON_SWORD' AND Namespace = 'champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('IRON_AXE', 'champions', 'standard_axe', '<yellow>Standard Axe', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'IRON_AXE' AND Namespace = 'champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_SWORD', 'champions', 'power_sword', '<yellow>Power Sword', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'DIAMOND_SWORD' AND Namespace = 'champions'), 0, '<gray>Damage: <green>7');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_AXE', 'champions', 'power_axe', '<yellow>Power Axe', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'DIAMOND_AXE' AND Namespace = 'champions'), 0, '<gray>Damage: <green>7');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_SWORD', 'champions', 'ancient_sword', '<yellow>Ancient Sword', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_SWORD' AND Namespace = 'champions'), 0, '<gray>Damage: <green>7');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_SWORD' AND Namespace = 'champions'), 1, '<gray>+1 Level to Sword Skills');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_AXE', 'champions', 'ancient_axe', '<yellow>Ancient Axe', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_AXE' AND Namespace = 'champions'), 0, '<gray>Damage: <green>7');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_AXE' AND Namespace = 'champions'), 1, '<gray>+1 Level to Axe Skills');


INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('STONE_SWORD', 'champions', 'basic_sword', '<yellow>Basic Sword', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'STONE_SWORD' AND Namespace = 'champions'), 0, '<gray>Damage: <green>5');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('STONE_AXE', 'champions', 'basic_axe', '<yellow>Basic Axe', 500, 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'STONE_AXE' AND Namespace = 'champions'), 0, '<gray>Damage: <green>5');

-- Tools

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_PICKAXE', 'champions', 'wooden_pickaxe', '<yellow>Wooden Pickaxe', 500, 0, 0, 0), ('STONE_PICKAXE', 'champions', 'stone_pickaxe', '<yellow>Stone Pickaxe', 500, 0, 0, 0),
    ('IRON_PICKAXE', 'champions', 'iron_pickaxe', '<yellow>Iron Pickaxe', 500, 0, 0, 0), ('GOLDEN_PICKAXE', 'champions', 'gold_pickaxe', '<yellow>Gold Pickaxe', 500, 0, 0, 0),
    ('DIAMOND_PICKAXE', 'champions', 'diamond_pickaxe', '<yellow>Diamond Pickaxe', 500, 0, 0, 0), ('NETHERITE_PICKAXE', 'champions', 'netherite_pickaxe', '<yellow>Netherite Pickaxe', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_AXE', 'champions', 'wooden_axe', '<yellow>Wooden Axe', 500, 0, 0, 0), ('STONE_AXE', 'champions', 'stone_axe', '<yellow>Stone Axe', 500, 0, 0, 0);
    
INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_HOE', 'champions', 'wooden_hoe', '<yellow>Wooden Hoe', 500, 0, 0, 0), ('STONE_HOE', 'champions', 'stone_hoe', '<yellow>Stone Hoe', 500, 0, 0, 0),
    ('IRON_HOE', 'champions', 'iron_hoe', '<yellow>Iron Hoe', 500, 0, 0, 0), ('GOLDEN_HOE', 'champions', 'gold_hoe', '<yellow>Gold Hoe', 500, 0, 0, 0),
    ('DIAMOND_HOE', 'champions', 'diamond_hoe', '<yellow>Diamond Hoe', 500, 0, 0, 0), ('NETHERITE_HOE', 'champions', 'netherite_hoe', '<yellow>Netherite Hoe', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_SHOVEL', 'champions', 'wooden_shovel', '<yellow>Wooden Shovel', 500, 0, 0, 0), ('STONE_SHOVEL', 'champions', 'stone_shovel', '<yellow>Stone Shovel', 500, 0, 0, 0),
    ('IRON_SHOVEL', 'champions', 'iron_shovel', '<yellow>Iron Shovel', 500, 0, 0, 0), ('GOLDEN_SHOVEL', 'champions', 'gold_shovel', '<yellow>Gold Shovel', 500, 0, 0, 0),
    ('DIAMOND_SHOVEL', 'champions', 'diamond_shovel', '<yellow>Diamond Shovel', 500, 0, 0, 0), ('NETHERITE_SHOVEL', 'champions', 'netherite_shovel', '<yellow>Netherite Shovel', 500, 0, 0, 0);

-- Custom weapons
INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('COBWEB', 'champions', 'throwing_web', '<light_purple>Throwing Web', 500, 0, 0, 0);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('APPLE', 'champions', 'energy_apple', '<light_purple>Energy Apple', 500, 0, 0, 0);

-- Legendaries
INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_MELLOHI', 'champions', 'wind_blade', '<orange>Wind Blade', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_MALL', 'champions', 'alligators_tooth', '<orange>Alligator\'s Tooth', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_BLOCKS', 'champions', 'hyper_axe', '<orange>Hyper Axe', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_WAIT', 'champions', 'knights_greatlance', '<orange>Knight\'s Greatlance', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_FAR', 'champions', 'magnetic_maul', '<orange>Magnetic Maul', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_CAT', 'champions', 'giants_broadsword', '<orange>Giant\'s Broadsword', 500, 1, 0, 1);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, Durability, ModelData, Glow, HasUUID) VALUES
    ('ELYTRA', 'champions', 'wings_of_zanzul', '<orange>Wings of Zanzul', 500, 1, 0, 1);
