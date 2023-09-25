-- Class armours
INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("LEATHER_HELMET", "Champions", "<yellow>Assassin Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("LEATHER_CHESTPLATE", "Champions", "<yellow>Assassin Vest", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("LEATHER_LEGGINGS", "Champions", "<yellow>Assassin Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("LEATHER_BOOTS", "Champions", "<yellow>Assassin Boots", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_HELMET", "Champions", "<yellow>Knight Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_CHESTPLATE", "Champions", "<yellow>Knight Vest", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_LEGGINGS", "Champions", "<yellow>Knight Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_BOOTS", "Champions", "<yellow>Knight Boots", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_HELMET", "Champions", "<yellow>Warlock Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_CHESTPLATE", "Champions", "<yellow>Warlock Chestplate", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_LEGGINGS", "Champions", "<yellow>Warlock Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_BOOTS", "Champions", "<yellow>Warlock Boots", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("CHAINMAIL_HELMET", "Champions", "<yellow>Ranger Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("CHAINMAIL_CHESTPLATE", "Champions", "<yellow>Ranger Vest", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("CHAINMAIL_LEGGINGS", "Champions", "<yellow>Ranger Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("CHAINMAIL_BOOTS", "Champions", "<yellow>Ranger Boots", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_HELMET", "Champions", "<yellow>Paladin Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_CHESTPLATE", "Champions", "<yellow>Paladin Vest", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_LEGGINGS", "Champions", "<yellow>Paladin Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_BOOTS", "Champions", "<yellow>Paladin Boots", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_HELMET", "Champions", "<yellow>Gladiator Helmet", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_CHESTPLATE", "Champions", "<yellow>Gladiator Chestplate", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_LEGGINGS", "Champions", "<yellow>Gladiator Leggings", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_BOOTS", "Champions", "<yellow>Gladiator Boots", 0);

-- Weapons
INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_AXE", "Champions", "<yellow>Booster Axe", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "GOLDEN_AXE" AND Module = "Champions"), 0, "<gray>Damage: <green>5");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("GOLDEN_SWORD", "Champions", "<yellow>Booster Sword", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "GOLDEN_SWORD" AND Module = "Champions"), 0, "<gray>Damage: <green>6");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_SWORD", "Champions", "<yellow>Standard Sword", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "IRON_SWORD" AND Module = "Champions"), 0, "<gray>Damage: <green>4.5");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("IRON_AXE", "Champions", "<yellow>Standard Axe", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "IRON_AXE" AND Module = "Champions"), 0, "<gray>Damage: <green>3");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_SWORD", "Champions", "<yellow>Power Sword", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "DIAMOND_SWORD" AND Module = "Champions"), 0, "<gray>Damage: <green>5");

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "DIAMOND_SWORD" AND Module = "Champions"), 1, "<gray>+1 Level to Sword Skills");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("DIAMOND_AXE", "Champions", "<yellow>Power Axe", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "DIAMOND_AXE" AND Module = "Champions"), 0, "<gray>Damage: <green>4");

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "DIAMOND_AXE" AND Module = "Champions"), 1, "<gray>+1 Level to Axe Skills");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_SWORD", "Champions", "<yellow>Ancient Sword", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "NETHERITE_SWORD" AND Module = "Champions"), 0, "<gray>Damage: <green>6");

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "NETHERITE_SWORD" AND Module = "Champions"), 1, "<gray>+1 Level to Sword Skills");

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("NETHERITE_AXE", "Champions", "<yellow>Ancient Axe", 0);

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "NETHERITE_AXE" AND Module = "Champions"), 0, "<gray>Damage: <green>5");

INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Material = "NETHERITE_AXE" AND Module = "Champions"), 1, "<gray>+1 Level to Axe Skills");


-- Tools

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("WOODEN_PICKAXE", "Champions", "<yellow>Wooden Pickaxe", 0), ("STONE_PICKAXE", "Champions", "<yellow>Stone Pickaxe", 0),
    ("IRON_PICKAXE", "Champions", "<yellow>Iron Pickaxe", 0), ("GOLD_PICKAXE", "Champions", "<yellow>Gold Pickaxe", 0),
    ("DIAMOND_PICKAXE", "Champions", "<yellow>Diamond Pickaxe", 0), ("NETHERITE_PICKAXE", "Champions", "<yellow>Netherite Pickaxe", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("WOODEN_AXE", "Champions", "<yellow>Wooden Axe", 0), ("STONE_AXE", "Champions", "<yellow>Stone Axe", 0);
    
INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("WOODEN_HOE", "Champions", "<yellow>Wooden Hoe", 0), ("STONE_HOE", "Champions", "<yellow>Stone Hoe", 0),
    ("IRON_HOE", "Champions", "<yellow>Iron Hoe", 0), ("GOLD_HOE", "Champions", "<yellow>Gold Hoe", 0),
    ("DIAMOND_HOE", "Champions", "<yellow>Diamond Hoe", 0), ("NETHERITE_HOE", "Champions", "<yellow>Netherite Hoe", 0);

INSERT IGNORE INTO items (Material, Module, Name, Glow) VALUES
    ("WOODEN_SHOVEL", "Champions", "<yellow>Wooden Shovel", 0), ("STONE_SHOVEL", "Champions", "<yellow>Stone Shovel", 0),
    ("IRON_SHOVEL", "Champions", "<yellow>Iron Shovel", 0), ("GOLD_SHOVEL", "Champions", "<yellow>Gold Shovel", 0),
    ("DIAMOND_SHOVEL", "Champions", "<yellow>Diamond Shovel", 0), ("NETHERITE_SHOVEL", "Champions", "<yellow>Netherite Shovel", 0);
