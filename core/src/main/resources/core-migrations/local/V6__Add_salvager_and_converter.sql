INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, ModelData, Glow, HasUUID) VALUES
    ('STONECUTTER', 'core', 'salvager', '<yellow>Salvager', 0, 0, 0);
INSERT IGNORE INTO itemlore
VALUES ((SELECT id FROM items WHERE Keyname = 'salvager'), 0, '<gray>Can be used to salvage weapons, tools, and armor.');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, ModelData, Glow, HasUUID) VALUES
    ('GRINDSTONE', 'core', 'resourceconverter', '<yellow>Resource Converter', 0, 0, 0);
INSERT IGNORE INTO itemlore
VALUES ((SELECT id FROM items WHERE Keyname = 'resourceconverter'), 0, '<gray>Can be used to exchange resources for other resources.');