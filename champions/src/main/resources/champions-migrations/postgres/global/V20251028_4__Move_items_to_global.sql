-- Class armours
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('LEATHER_HELMET', 'champions', 'assassin_helmet', '<yellow>Assassin Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('LEATHER_CHESTPLATE', 'champions', 'assassin_vest', '<yellow>Assassin Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('LEATHER_LEGGINGS', 'champions', 'assassin_leggings', '<yellow>Assassin Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('LEATHER_BOOTS', 'champions', 'assassin_boots', '<yellow>Assassin Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_boots'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_HELMET', 'champions', 'knight_helmet', '<yellow>Knight Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_CHESTPLATE', 'champions', 'knight_vest', '<yellow>Knight Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_LEGGINGS', 'champions', 'knight_leggings', '<yellow>Knight Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_BOOTS', 'champions', 'knight_boots', '<yellow>Knight Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_boots'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_HELMET', 'champions', 'warlock_helmet', '<yellow>Warlock Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_CHESTPLATE', 'champions', 'warlock_vest', '<yellow>Warlock Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_LEGGINGS', 'champions', 'warlock_leggings', '<yellow>Warlock Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_BOOTS', 'champions', 'warlock_boots', '<yellow>Warlock Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_boots'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('CHAINMAIL_HELMET', 'champions', 'ranger_helmet', '<yellow>Ranger Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('CHAINMAIL_CHESTPLATE', 'champions', 'ranger_vest', '<yellow>Ranger Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('CHAINMAIL_LEGGINGS', 'champions', 'ranger_leggings', '<yellow>Ranger Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('CHAINMAIL_BOOTS', 'champions', 'ranger_boots', '<yellow>Ranger Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_boots'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_HELMET', 'champions', 'mage_helmet', '<yellow>Mage Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_CHESTPLATE', 'champions', 'mage_vest', '<yellow>Mage Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_LEGGINGS', 'champions', 'mage_leggings', '<yellow>Mage Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_BOOTS', 'champions', 'mage_boots', '<yellow>Mage Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_boots'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_HELMET', 'champions', 'brute_helmet', '<yellow>Brute Helmet', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_helmet'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_CHESTPLATE', 'champions', 'brute_vest', '<yellow>Brute Vest', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_vest'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_LEGGINGS', 'champions', 'brute_leggings', '<yellow>Brute Leggings', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_leggings'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_BOOTS', 'champions', 'brute_boots', '<yellow>Brute Boots', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_boots'), 750)
ON CONFLICT DO NOTHING;

-- Weapons
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_AXE', 'champions', 'booster_axe', '<yellow>Booster Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'booster_axe'), 1000)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'GOLDEN_AXE' AND namespace = 'champions'), 0, '<gray>Damage: <green>6')
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'GOLDEN_AXE' AND namespace = 'champions'), 1, '<gray>+1 Level to Axe Skills')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GOLDEN_SWORD', 'champions', 'booster_sword', '<yellow>Booster Sword', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'booster_sword'), 1000)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'GOLDEN_SWORD' AND namespace = 'champions'), 0, '<gray>Damage: <green>6')
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'GOLDEN_SWORD' AND namespace = 'champions'), 1, '<gray>+1 Level to Sword Skills')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_SWORD', 'champions', 'standard_sword', '<yellow>Standard Sword', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'standard_sword'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'IRON_SWORD' AND namespace = 'champions'), 0, '<gray>Damage: <green>6')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('IRON_AXE', 'champions', 'standard_axe', '<yellow>Standard Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'standard_axe'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'IRON_AXE' AND namespace = 'champions'), 0, '<gray>Damage: <green>6')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_SWORD', 'champions', 'power_sword', '<yellow>Power Sword', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'power_sword'), 1000)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'DIAMOND_SWORD' AND namespace = 'champions'), 0, '<gray>Damage: <green>7')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('DIAMOND_AXE', 'champions', 'power_axe', '<yellow>Power Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'power_axe'), 1000)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'DIAMOND_AXE' AND namespace = 'champions'), 0, '<gray>Damage: <green>7')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_SWORD', 'champions', 'ancient_sword', '<yellow>Ancient Sword', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_sword'), 1250)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'NETHERITE_SWORD' AND namespace = 'champions'), 0, '<gray>Damage: <green>7')
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'NETHERITE_SWORD' AND namespace = 'champions'), 1, '<gray>+1 Level to Sword Skills')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('NETHERITE_AXE', 'champions', 'ancient_axe', '<yellow>Ancient Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_axe'), 1250)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'NETHERITE_AXE' AND namespace = 'champions'), 0, '<gray>Damage: <green>7')
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'NETHERITE_AXE' AND namespace = 'champions'), 1, '<gray>+1 Level to Axe Skills')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('STONE_SWORD', 'champions', 'basic_sword', '<yellow>Basic Sword', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'basic_sword'), 400)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'STONE_SWORD' AND namespace = 'champions'), 0, '<gray>Damage: <green>5')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('STONE_AXE', 'champions', 'basic_axe', '<yellow>Basic Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'basic_axe'), 400)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'STONE_AXE' AND namespace = 'champions'), 0, '<gray>Damage: <green>5')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('CROSSBOW', 'champions', 'crossbow', '<yellow>Crossbow', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'crossbow'), 1000)
ON CONFLICT DO NOTHING;

INSERT INTO itemlore
VALUES ((SELECT id FROM items WHERE material = 'CROSSBOW' AND namespace = 'champions'), 0, '<gray>+1 Level to Bow Skills')
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('BOW', 'champions', 'bow', '<yellow>Bow', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO itemdurability
VALUES ((SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'bow'), 750)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('ARROW', 'champions', 'arrow', '<yellow>Arrow', 0, 0, 0)
ON CONFLICT DO NOTHING;

-- Tools

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
                                                                                     ('WOODEN_PICKAXE', 'champions', 'wooden_pickaxe', '<yellow>Wooden Pickaxe', 0, 0, 0),
                                                                                     ('STONE_PICKAXE', 'champions', 'stone_pickaxe', '<yellow>Stone Pickaxe', 0, 0, 0),
                                                                                     ('IRON_PICKAXE', 'champions', 'iron_pickaxe', '<yellow>Iron Pickaxe', 0, 0, 0),
                                                                                     ('GOLDEN_PICKAXE', 'champions', 'gold_pickaxe', '<yellow>Gold Pickaxe', 0, 0, 0),
                                                                                     ('DIAMOND_PICKAXE', 'champions', 'diamond_pickaxe', '<yellow>Diamond Pickaxe', 0, 0, 0),
                                                                                     ('NETHERITE_PICKAXE', 'champions', 'netherite_pickaxe', '<yellow>Netherite Pickaxe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
                                                                                     ('WOODEN_AXE', 'champions', 'wooden_axe', '<yellow>Wooden Axe', 0, 0, 0),
                                                                                     ('STONE_AXE', 'champions', 'stone_axe', '<yellow>Stone Axe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
                                                                                     ('WOODEN_HOE', 'champions', 'wooden_hoe', '<yellow>Wooden Hoe', 0, 0, 0),
                                                                                     ('STONE_HOE', 'champions', 'stone_hoe', '<yellow>Stone Hoe', 0, 0, 0),
                                                                                     ('IRON_HOE', 'champions', 'iron_hoe', '<yellow>Iron Hoe', 0, 0, 0),
                                                                                     ('GOLDEN_HOE', 'champions', 'gold_hoe', '<yellow>Gold Hoe', 0, 0, 0),
                                                                                     ('DIAMOND_HOE', 'champions', 'diamond_hoe', '<yellow>Diamond Hoe', 0, 0, 0),
                                                                                     ('NETHERITE_HOE', 'champions', 'netherite_hoe', '<yellow>Netherite Hoe', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
                                                                                     ('WOODEN_SHOVEL', 'champions', 'wooden_shovel', '<yellow>Wooden Shovel', 0, 0, 0),
                                                                                     ('STONE_SHOVEL', 'champions', 'stone_shovel', '<yellow>Stone Shovel', 0, 0, 0),
                                                                                     ('IRON_SHOVEL', 'champions', 'iron_shovel', '<yellow>Iron Shovel', 0, 0, 0),
                                                                                     ('GOLDEN_SHOVEL', 'champions', 'gold_shovel', '<yellow>Gold Shovel', 0, 0, 0),
                                                                                     ('DIAMOND_SHOVEL', 'champions', 'diamond_shovel', '<yellow>Diamond Shovel', 0, 0, 0),
                                                                                     ('NETHERITE_SHOVEL', 'champions', 'netherite_shovel', '<yellow>Netherite Shovel', 0, 0, 0)
ON CONFLICT DO NOTHING;

-- Custom weapons
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('COBWEB', 'champions', 'throwing_web', '<light_purple>Throwing Web', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('APPLE', 'champions', 'energy_apple', '<light_purple>Energy Apple', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('HONEY_BOTTLE', 'champions', 'energy_elixir', '<light_purple>Energy Elixir', 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('COOKIE', 'champions', 'purification_potion', '<light_purple>Purification Potion', 1, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('PUMPKIN_PIE', 'champions', 'mushroom_stew', '<light_purple>Mushroom Stew', 1, 0, 0)
ON CONFLICT DO NOTHING;

-- Legendaries
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_MELLOHI', 'champions', 'wind_blade', '<orange>Wind Blade', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('BREAD', 'champions', 'rake', '<orange>Rake', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_MALL', 'champions', 'alligators_tooth', '<orange>Alligator''s Tooth', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_BLOCKS', 'champions', 'hyper_axe', '<orange>Hyper Axe', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('SHIELD', 'champions', 'thunderclap_aegis', '<orange>Thunderclap Aegis', 99, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_STAL', 'champions', 'scythe', '<orange>Scythe of the Fallen Lord', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_CHIRP', 'champions', 'scepter', '<orange>Meridian Scepter', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_FAR', 'champions', 'magnetic_maul', '<orange>Magnetic Maul', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MUSIC_DISC_CAT', 'champions', 'giants_broadsword', '<orange>Giant''s Broadsword', 1, 0, 1)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('FIREWORK_STAR', 'champions', 'runed_pickaxe', '<orange>Runed Pickaxe', 1, 0, 1)
ON CONFLICT DO NOTHING;

-- Add runes to items
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RAISER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'conquering_rune_t1', '<green>Rune of Conquering I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RAISER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'conquering_rune_t2', '<blue>Rune of Conquering II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RAISER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'conquering_rune_t3', '<yellow>Rune of Conquering III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('WARD_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'reinforcing_rune_t1', '<green>Rune of Reinforcing I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('WARD_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'reinforcing_rune_t2', '<blue>Rune of Reinforcing II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('WARD_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'reinforcing_rune_t3', '<yellow>Rune of Reinforcing III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COAST_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'resistance_rune_t1', '<green>Rune of Resistance I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COAST_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'resistance_rune_t2', '<blue>Rune of Resistance II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COAST_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'resistance_rune_t3', '<yellow>Rune of Resistance III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('EYE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'power_rune_t1', '<green>Rune of Power I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('EYE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'power_rune_t2', '<blue>Rune of Power II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('EYE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'power_rune_t3', '<yellow>Rune of Power III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('VEX_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'insight_rune_t1', '<green>Rune of Insight I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('VEX_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'insight_rune_t2', '<blue>Rune of Insight II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('VEX_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'insight_rune_t3', '<yellow>Rune of Insight III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'haste_rune_t4', '<orange>Rune of Haste IV', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RIB_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'unbreaking_rune_t1', '<green>Rune of Unbreaking I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RIB_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'unbreaking_rune_t2', '<blue>Rune of Unbreaking II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RIB_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'unbreaking_rune_t3', '<yellow>Rune of Unbreaking III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('RIB_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'unbreaking_rune_t4', '<orange>Rune of Unbreaking IV', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'alacrity_rune_t4', '<orange>Rune of Alacrity IV', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'frost_rune_t3', '<yellow>Rune of Frost III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'frost_rune_t4', '<orange>Rune of Frost IV', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('DUNE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'scorching_rune_t3', '<yellow>Rune of Scorching III', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('DUNE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'scorching_rune_t4', '<orange>Rune of Scorching IV', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'fortune_rune_t1', '<green>Rune of Fortune I', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'fortune_rune_t2', '<blue>Rune of Fortune II', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE', 'champions', 'fortune_rune_t3', '<yellow>Rune of Fortune III', 3, 0, 0)
ON CONFLICT DO NOTHING;

-- New Consumables
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GLOW_BERRIES', 'champions', 'rabbit_stew', '<light_purple>Rabbit Stew', 1, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('MELON_SLICE', 'champions', 'suspicious_stew', '<light_purple>Suspicious Stew', 1, 0, 0)
ON CONFLICT DO NOTHING;

UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_helmet');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_vest');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_leggings');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_boots');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'standard_axe');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'standard_sword');
UPDATE itemdurability
SET durability = 200
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'basic_axe');
UPDATE itemdurability
SET durability = 200
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'basic_sword');
UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'booster_axe');
UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'booster_sword');
UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'power_axe');
UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'power_sword');
UPDATE itemdurability
SET durability = 1000
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_axe');
UPDATE itemdurability
SET durability = 1000
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_sword');
UPDATE itemdurability
SET durability = 500
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'bow');
UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'crossbow');

UPDATE items SET material = 'SPIDER_EYE', model_data = 2 WHERE namespace = 'champions' AND keyname = 'rabbit_stew';
UPDATE items SET material = 'ROTTEN_FLESH', model_data = 3 WHERE namespace = 'champions' AND keyname = 'suspicious_stew';

UPDATE items SET material = 'MUSIC_DISC_WARD', model_data = 2 WHERE namespace = 'champions' AND keyname = 'rake';
UPDATE items SET material = 'MUSIC_DISC_WARD', model_data = 1 WHERE namespace = 'champions' AND keyname = 'runed_pickaxe';

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'assassin_boots');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'knight_boots');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'mage_boots');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ranger_boots');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'brute_boots');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_helmet');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_vest');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_leggings');

UPDATE itemdurability
SET durability = 650
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'warlock_boots');

UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_axe');

UPDATE itemdurability
SET durability = 750
WHERE item = (SELECT id FROM items WHERE namespace = 'champions' AND keyname = 'ancient_sword');

UPDATE items SET keyname = 'mitigation_rune_t1', Name = '<green>Rune of Mitigation I'
WHERE namespace = 'champions'
  AND keyname = 'resistance_rune_t1';
UPDATE items SET keyname = 'mitigation_rune_t2', Name = '<blue>Rune of Mitigation II'
WHERE namespace = 'champions'
  AND keyname = 'resistance_rune_t2';
UPDATE items SET keyname = 'mitigation_rune_t3', Name = '<yellow>Rune of Mitigation III'
WHERE namespace = 'champions'
  AND keyname = 'resistance_rune_t3';

UPDATE itemlore SET text = '<gray>Damage: <green>6' WHERE item = (SELECT id FROM items WHERE material = 'NETHERITE_SWORD') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>6' WHERE item = (SELECT id FROM items WHERE material = 'NETHERITE_AXE') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>6' WHERE item = (SELECT id FROM items WHERE material = 'DIAMOND_SWORD') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>6' WHERE item = (SELECT id FROM items WHERE material = 'DIAMOND_AXE') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>5' WHERE item = (SELECT id FROM items WHERE material = 'GOLDEN_SWORD') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>5' WHERE item = (SELECT id FROM items WHERE material = 'GOLDEN_AXE') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>5' WHERE item = (SELECT id FROM items WHERE material = 'IRON_SWORD') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>5' WHERE item = (SELECT id FROM items WHERE material = 'IRON_AXE') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>4' WHERE item = (SELECT id FROM items WHERE material = 'STONE_SWORD') AND priority = 0;
UPDATE itemlore SET text = '<gray>Damage: <green>4' WHERE item = (SELECT id FROM items WHERE material = 'STONE_AXE') AND priority = 0;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('STICK', 'champions', 'ghost_handle', '<yellow>Ghost Handle', 2, 0, 0)
ON CONFLICT DO NOTHING;