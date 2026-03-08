INSERT INTO shopitems_flags (shop_item_id, persistent_key, persistent_value)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Lumberjack' AND material = 'DIAMOND_AXE'), 'SHOP_CURRENCY', 'BARK')
ON CONFLICT DO NOTHING;

INSERT INTO shopitems_flags (shop_item_id, persistent_key, persistent_value)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Lumberjack' AND material = 'GOLDEN_AXE'), 'SHOP_CURRENCY', 'BARK')
ON CONFLICT DO NOTHING;

INSERT INTO shopitems_flags (shop_item_id, persistent_key, persistent_value)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Lumberjack' AND material = 'IRON_AXE'), 'SHOP_CURRENCY', 'BARK')
ON CONFLICT DO NOTHING;

INSERT INTO shopitems_flags (shop_item_id, persistent_key, persistent_value)
VALUES ((SELECT id FROM shopitems WHERE shopkeeper = 'Lumberjack' AND material = 'MANGROVE_PROPAGULE'), 'SHOP_CURRENCY', 'BARK')
ON CONFLICT DO NOTHING;