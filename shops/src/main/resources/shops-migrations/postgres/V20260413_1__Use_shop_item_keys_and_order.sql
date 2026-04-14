ALTER TABLE shopitems
    ADD COLUMN item_key VARCHAR(255);

ALTER TABLE shopitems
    ADD COLUMN "order" INT;

DELETE FROM shopitems WHERE material = 'FLINT_AND_STEEL';

UPDATE shopitems
SET item_key = CASE
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Trout' THEN 'progression:trout'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Salmon' THEN 'progression:salmon'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Bluegill' THEN 'progression:bluegill'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Gar' THEN 'progression:gar'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Carp' THEN 'progression:carp'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Catfish' THEN 'progression:catfish'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Cod' THEN 'progression:cod'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Drum' THEN 'progression:drum'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Sablefish' THEN 'progression:sablefish'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Kingfish' THEN 'progression:kingfish'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Cobia' THEN 'progression:cobia'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Sea Bass' THEN 'progression:sea_bass'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Tuna' THEN 'progression:tuna'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Swordfish' THEN 'progression:swordfish'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Marlin' THEN 'progression:marlin'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Grouper' THEN 'progression:grouper'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Sturgeon' THEN 'progression:sturgeon'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Sunfish' THEN 'progression:sunfish'
                   WHEN shopkeeper = 'Fisherman' AND item_name = 'Speedy Bait' THEN 'progression:speedy_bait'
                   WHEN shopkeeper = 'Weapons / Tools' AND item_name = 'Power Sword' THEN 'core:power_sword'
                   WHEN shopkeeper = 'Weapons / Tools' AND item_name = 'Booster Sword' THEN 'core:booster_sword'
                   WHEN shopkeeper = 'Weapons / Tools' AND item_name = 'Standard Sword' THEN 'core:standard_sword'
                   WHEN item_name = 'Power Axe' THEN 'core:power_axe'
                   WHEN item_name = 'Booster Axe' THEN 'core:booster_axe'
                   WHEN item_name = 'Standard Axe' THEN 'core:standard_axe'
                   WHEN item_name = 'Diamond Pickaxe' THEN 'core:power_pickaxe'
                   WHEN item_name = 'Diamond Shovel' THEN 'core:power_shovel'
                   WHEN item_name = 'Diamond Hoe' THEN 'core:power_hoe'
                   WHEN item_name = 'Iron Pickaxe' THEN 'core:standard_pickaxe'
                   WHEN item_name = 'Iron Shovel' THEN 'core:standard_shovel'
                   WHEN item_name = 'Iron Hoe' THEN 'core:standard_hoe'
                   WHEN item_name = 'Fishing Rod' THEN 'core:fishing_rod'
                   WHEN item_name = 'Bow' THEN 'core:bow'
                   WHEN item_name = 'Cannon' THEN 'core:cannon'
                   WHEN item_name = 'Cannonball' THEN 'core:cannonball'
                   WHEN item_name = 'Coal' THEN 'core:coal'
                   WHEN item_name = 'Coal Block' THEN 'core:coal_block'
                   WHEN item_name = 'Compacted Log' THEN 'progression:compacted_log'
                   WHEN item_name = 'Energy Apple' THEN 'champions:energy_apple'
                   WHEN item_name = 'Rabbit Stew' THEN 'champions:rabbit_stew'
                   WHEN item_name = 'Mushroom Stew' THEN 'champions:mushroom_stew'
                   WHEN item_name = 'Suspicious Stew' THEN 'champions:suspicious_stew'
                   WHEN item_name = 'Purifying Capsule' THEN 'champions:purification_potion'
                   WHEN item_name = 'Throwing Web' THEN 'champions:throwing_web'
                   WHEN item_name = 'Enchantment Table' THEN 'champions:build_editor'
                   WHEN item_name = 'Anvil' THEN 'core:anvil'
                   ELSE 'minecraft:' || lower(material)
               END
WHERE item_key IS NULL;

UPDATE shopitems
SET "order" = ((COALESCE(menu_page, 1) - 1) * 45) + menu_slot
WHERE item_key IS NOT NULL AND menu_slot IS NOT NULL;

ALTER TABLE shopitems
    DROP CONSTRAINT IF EXISTS shopitems_shopkeeper_material_itemname_uk;

ALTER TABLE shopitems
    ALTER COLUMN item_key SET NOT NULL;

ALTER TABLE shopitems
    ALTER COLUMN "order" SET NOT NULL;

ALTER TABLE shopitems
    DROP COLUMN material;

ALTER TABLE shopitems
    DROP COLUMN item_name;

ALTER TABLE shopitems
    DROP COLUMN model_data;

ALTER TABLE shopitems
    DROP COLUMN menu_slot;

ALTER TABLE shopitems
    DROP COLUMN menu_page;

ALTER TABLE shopitems
    DROP COLUMN amount;

ALTER TABLE shopitems
    ADD CONSTRAINT shopitems_shopkeeper_item_key_order_uk UNIQUE (shopkeeper, item_key, "order");
