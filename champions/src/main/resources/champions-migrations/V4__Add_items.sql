-- Class armours
INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_HELMET', 'Champions', '<yellow>Assassin Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_CHESTPLATE', 'Champions', '<yellow>Assassin Vest', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_LEGGINGS', 'Champions', '<yellow>Assassin Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('LEATHER_BOOTS', 'Champions', '<yellow>Assassin Boots', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_HELMET', 'Champions', '<yellow>Knight Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_CHESTPLATE', 'Champions', '<yellow>Knight Vest', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_LEGGINGS', 'Champions', '<yellow>Knight Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_BOOTS', 'Champions', '<yellow>Knight Boots', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_HELMET', 'Champions', '<yellow>Warlock Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_CHESTPLATE', 'Champions', '<yellow>Warlock Chestplate', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_LEGGINGS', 'Champions', '<yellow>Warlock Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_BOOTS', 'Champions', '<yellow>Warlock Boots', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_HELMET', 'Champions', '<yellow>Ranger Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_CHESTPLATE', 'Champions', '<yellow>Ranger Vest', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_LEGGINGS', 'Champions', '<yellow>Ranger Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('CHAINMAIL_BOOTS', 'Champions', '<yellow>Ranger Boots', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_HELMET', 'Champions', '<yellow>Paladin Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_CHESTPLATE', 'Champions', '<yellow>Paladin Vest', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_LEGGINGS', 'Champions', '<yellow>Paladin Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_BOOTS', 'Champions', '<yellow>Paladin Boots', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_HELMET', 'Champions', '<yellow>Gladiator Helmet', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_CHESTPLATE', 'Champions', '<yellow>Gladiator Chestplate', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_LEGGINGS', 'Champions', '<yellow>Gladiator Leggings', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_BOOTS', 'Champions', '<yellow>Gladiator Boots', 0, 0, 0);

-- Weapons
INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_AXE', 'Champions', '<yellow>Booster Axe', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_AXE' AND Module = 'Champions'), 0, '<gray>Damage: <green>5');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_AXE' AND Module = 'Champions'), 1, '<gray>+1 Level to Axe Skills');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('GOLDEN_SWORD', 'Champions', '<yellow>Booster Sword', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_SWORD' AND Module = 'Champions'), 0, '<gray>Damage: <green>5');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'GOLDEN_SWORD' AND Module = 'Champions'), 1, '<gray>+1 Level to Sword Skills');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_SWORD', 'Champions', '<yellow>Standard Sword', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'IRON_SWORD' AND Module = 'Champions'), 0, '<gray>Damage: <green>5');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('IRON_AXE', 'Champions', '<yellow>Standard Axe', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'IRON_AXE' AND Module = 'Champions'), 0, '<gray>Damage: <green>5');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_SWORD', 'Champions', '<yellow>Power Sword', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'DIAMOND_SWORD' AND Module = 'Champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('DIAMOND_AXE', 'Champions', '<yellow>Power Axe', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'DIAMOND_AXE' AND Module = 'Champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_SWORD', 'Champions', '<yellow>Ancient Sword', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_SWORD' AND Module = 'Champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_SWORD' AND Module = 'Champions'), 1, '<gray>+1 Level to Sword Skills');

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('NETHERITE_AXE', 'Champions', '<yellow>Ancient Axe', 0, 0, 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_AXE' AND Module = 'Champions'), 0, '<gray>Damage: <green>6');

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = 'NETHERITE_AXE' AND Module = 'Champions'), 1, '<gray>+1 Level to Axe Skills');


-- Tools

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_PICKAXE', 'Champions', '<yellow>Wooden Pickaxe', 0, 0, 0), ('STONE_PICKAXE', 'Champions', '<yellow>Stone Pickaxe', 0, 0, 0),
    ('IRON_PICKAXE', 'Champions', '<yellow>Iron Pickaxe', 0, 0, 0), ('GOLDEN_PICKAXE', 'Champions', '<yellow>Gold Pickaxe', 0, 0, 0),
    ('DIAMOND_PICKAXE', 'Champions', '<yellow>Diamond Pickaxe', 0, 0, 0), ('NETHERITE_PICKAXE', 'Champions', '<yellow>Netherite Pickaxe', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_AXE', 'Champions', '<yellow>Wooden Axe', 0, 0, 0), ('STONE_AXE', 'Champions', '<yellow>Stone Axe', 0, 0, 0);
    
INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_HOE', 'Champions', '<yellow>Wooden Hoe', 0, 0, 0), ('STONE_HOE', 'Champions', '<yellow>Stone Hoe', 0, 0, 0),
    ('IRON_HOE', 'Champions', '<yellow>Iron Hoe', 0, 0, 0), ('GOLDEN_HOE', 'Champions', '<yellow>Gold Hoe', 0, 0, 0),
    ('DIAMOND_HOE', 'Champions', '<yellow>Diamond Hoe', 0, 0, 0), ('NETHERITE_HOE', 'Champions', '<yellow>Netherite Hoe', 0, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('WOODEN_SHOVEL', 'Champions', '<yellow>Wooden Shovel', 0, 0, 0), ('STONE_SHOVEL', 'Champions', '<yellow>Stone Shovel', 0, 0, 0),
    ('IRON_SHOVEL', 'Champions', '<yellow>Iron Shovel', 0, 0, 0), ('GOLDEN_SHOVEL', 'Champions', '<yellow>Gold Shovel', 0, 0, 0),
    ('DIAMOND_SHOVEL', 'Champions', '<yellow>Diamond Shovel', 0, 0, 0), ('NETHERITE_SHOVEL', 'Champions', '<yellow>Netherite Shovel', 0, 0, 0);

-- Custom weapons
INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('COBWEB', 'Champions', '<light_purple>Throwing Web', 1, 0, 0);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('APPLE', 'Champions', '<light_purple>Energy Apple', 0, 0, 0);

-- Legendaries
INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_MELLOHI', 'Champions', '<orange>Wind Blade', 1, 0, 1);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_MALL', 'Champions', '<orange>Alligators Tooth', 1, 0, 1);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('MUSIC_DISC_BLOCKS', 'Champions', '<orange>Hyper Axe', 1, 0, 1);

INSERT IGNORE INTO items (Material, Module, Name, ModelData, Glow, HasUUID) VALUES
    ('ELYTRA', 'Champions', '<orange>Wings of Zanzul', 1, 0, 1);
