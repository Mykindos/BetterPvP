UPDATE shopitems_dynamic_pricing
SET base_stock = 25000, max_stock = 50000, current_stock = 25000
WHERE shop_item_id IN (SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD');