-- Pumpkin
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'PUMPKIN'), 0, 10, 20, 30, 60, 70, 80, 100000, 200000, 100000)
ON CONFLICT DO NOTHING;

-- Cocoa Beans
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'COCOA_BEANS'), 0, 5, 25, 30, 40, 50, 60, 25000, 50000, 25000)
ON CONFLICT DO NOTHING;

-- Sugar Cane
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'SUGAR_CANE'), 0, 10, 20, 25, 30, 35, 40, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Melon
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'MELON'), 0, 10, 30, 40, 65, 75, 85, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Carrot
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'CARROT'), 0, 10, 25, 40, 40, 45, 50, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Potato
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'POTATO'), 0, 10, 25, 40, 40, 45, 50, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Nether Wart
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'NETHER_WART'), 0, 10, 25, 40, 45, 55, 65, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Wheat
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'WHEAT'), 0, 10, 25, 40, 50, 55, 60, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Beetroot
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'BEETROOT'), 0, 10, 25, 40, 50, 55, 60, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Sweet Berries
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'SWEET_BERRIES'), 0, 7, 23, 35, 40, 45, 50, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Honeycomb
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'HONEYCOMB'), 0, 400, 750, 1000, 3333, 3333, 3600, 5000, 10000, 5000)
ON CONFLICT DO NOTHING;

-- Beehive
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'BEEHIVE'), 0, 10, 25, 40, 50, 55, 60, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

UPDATE shopitems_dynamic_pricing SET min_sell_price = 50, base_sell_price = 500, max_sell_price = 1000, min_buy_price = 3333,
                                     base_buy_price = 3333, max_buy_price = 3333, max_stock = 15000, current_stock = 7500
WHERE shop_item_id = (SELECT id FROM shopitems WHERE material = 'HONEYCOMB');

-- Glow Berries
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Farming' AND material = 'GLOW_BERRIES'), 0, 10, 25, 40, 50, 55, 60, 50000, 100000, 50000)
ON CONFLICT DO NOTHING;

-- Mushroom Stew
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Weapons / Tools' AND item_name = 'Mushroom Stew'), 0, 100, 500, 900, 600, 1000, 1400, 25000, 50000, 25000)
ON CONFLICT DO NOTHING;

UPDATE shopitems_dynamic_pricing SET base_stock = 500, max_stock = 1000, current_stock = 500
WHERE shop_item_id = (SELECT id FROM shopitems WHERE shopkeeper = 'Weapons / Tools' AND item_name = 'Mushroom Stew');

-- Trout (model_data: 1)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 1), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Salmon (model_data: 2)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 2), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Bluegill (model_data: 3)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 3), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Gar (model_data: 4)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 4), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Carp (model_data: 5)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 5), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Catfish (model_data: 6)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 6), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Cod (model_data: 7)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 7), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Drum (model_data: 8)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 8), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Sablefish (model_data: 9)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 9), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Kingfish (model_data: 10)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 10), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Cobia (model_data: 11)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 11), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Sea Bass (model_data: 12)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 12), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Tuna (model_data: 13)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 13), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Swordfish (model_data: 14)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 14), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Marlin (model_data: 15)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 15), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Grouper (model_data: 16)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 16), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Sturgeon (model_data: 17)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 17), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;

-- Sunfish (model_data: 18)
INSERT INTO shopitems_dynamic_pricing (shop_item_id, realm, min_sell_price, base_sell_price, max_sell_price, min_buy_price, base_buy_price, max_buy_price, base_stock, max_stock, current_stock)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Fisherman' AND material = 'COD' AND model_data = 18), 0, 10, 25, 35, 35, 40, 45, 50000, 200000, 50000)
ON CONFLICT DO NOTHING;