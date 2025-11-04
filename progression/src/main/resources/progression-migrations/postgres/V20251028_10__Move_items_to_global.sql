-- Fish
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'trout', '<yellow>Trout', 1, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'salmon', '<yellow>Salmon', 2, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'bluegill', '<yellow>Bluegill', 3, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'gar', '<yellow>Gar', 4, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'carp', '<yellow>Carp', 5, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'catfish', '<yellow>Catfish', 6, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'cod', '<yellow>Cod', 7, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'drum', '<yellow>Drum', 8, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'sablefish', '<yellow>Sablefish', 9, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'kingfish', '<yellow>Kingfish', 10, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'cobia', '<yellow>Cobia', 11, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'sea_bass', '<yellow>Sea Bass', 12, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'tuna', '<yellow>Tuna', 13, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'swordfish', '<yellow>Swordfish', 14, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'marlin', '<yellow>Marlin', 15, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'grouper', '<yellow>Grouper', 16, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'sturgeon', '<yellow>Sturgeon', 17, 0, 0)
ON CONFLICT DO NOTHING;
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('COD', 'progression', 'sunfish', '<yellow>Sunfish', 18, 0, 0)
ON CONFLICT DO NOTHING;

-- Baits
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('ORANGE_GLAZED_TERRACOTTA', 'progression', 'speedy_bait', '<light_purple>Speedy Bait', 1, 0, 0)
ON CONFLICT DO NOTHING;

-- Rod
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('FISHING_ROD', 'progression', 'fishing_rod', '<yellow>Fishing Rod', 0, 0, 0)
ON CONFLICT DO NOTHING;

-- Woodcutting-related
INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('GLISTERING_MELON_SLICE', 'progression', 'tree_bark', '<aqua>Tree Bark', 1, 0, 0)
ON CONFLICT DO NOTHING;

UPDATE items SET model_data = 1 WHERE keyname = 'compacted_log' AND namespace = 'progression';

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('OAK_WOOD', 'progression', 'compacted_log', '<light_purple>Compacted Log', 0, 1, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('LIGHT_BLUE_GLAZED_TERRACOTTA', 'progression', 'event_bait', '<light_purple>Event Bait', 1, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES ('FISHING_ROD', 'progression', 'sharkbait', '<orange>Sharkbait', 1, 0, 1)
ON CONFLICT DO NOTHING;