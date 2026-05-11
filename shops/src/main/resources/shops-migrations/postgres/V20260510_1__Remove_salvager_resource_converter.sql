-- remove RESOURCE CONVERTER
DELETE FROM shopitems
WHERE shopkeeper = 'Resource Merchant' AND item_key = 'minecraft:grindstone';

-- remove SALVAGER
DELETE FROM shopitems
WHERE shopkeeper = 'Resource Merchant' AND item_key = 'minecraft:stonecutter';