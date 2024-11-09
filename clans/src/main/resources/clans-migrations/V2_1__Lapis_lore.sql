INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, ModelData, Glow, HasUUID) VALUES
    ('LAPIS_BLOCK', 'clans', 'water_block', '<yellow>Water Block', 0, 0, 0);
INSERT IGNORE INTO itemlore
    VALUES ((SELECT id FROM items WHERE Namespace = 'clans' AND Keyname = 'water_block'), 0, '<gray>When placed, this block turns into water!');