-- rename minecraft:iron_door to clans:iron_door
UPDATE shopitems
SET item_key = 'clans:iron_door'
WHERE shopkeeper = 'Block Merchant' AND item_key = 'minecraft:iron_door';

-- rename minecraft:iron_trapdoor to clans:iron_trapdoor
UPDATE shopitems
SET item_key = 'clans:iron_trapdoor'
WHERE shopkeeper = 'Block Merchant' AND item_key = 'minecraft:iron_trapdoor';

-- add core:hammer to Resource Merchant
INSERT INTO shopitems (shopkeeper, buy_price, sell_price, item_key, "order")
VALUES ('Resource Merchant', 1000, 0, 'core:hammer', 17);