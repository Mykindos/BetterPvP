CREATE TABLE IF NOT EXISTS items
(
    id         SERIAL PRIMARY KEY,
    material   VARCHAR(255) NOT NULL,
    namespace  VARCHAR(255) NOT NULL,
    keyname    VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    model_data INTEGER      NOT NULL DEFAULT 0,
    glow       SMALLINT     NOT NULL,
    has_uuid   SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT items_uk
        UNIQUE (material, namespace, model_data)
);

CREATE TABLE IF NOT EXISTS itemdurability
(
    item       INTEGER NOT NULL PRIMARY KEY REFERENCES items (id) ON DELETE CASCADE,
    durability INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS itemlore
(
    item     INTEGER      NOT NULL REFERENCES items (id),
    priority INTEGER      NOT NULL DEFAULT 0,
    text     VARCHAR(255) NOT NULL,
    CONSTRAINT itemlore_item_priority_uk
        UNIQUE (item, priority)
);

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('STONECUTTER', 'core', 'salvager', '<yellow>Salvager', 0, 0, 0)
ON CONFLICT (material, namespace, model_data) DO NOTHING;

INSERT INTO itemlore (item, priority, text)
VALUES ((SELECT id FROM items WHERE keyname = 'salvager'), 0, '<gray>Can be used to salvage weapons, tools, and armor.')
ON CONFLICT (item, priority) DO NOTHING;

INSERT INTO items (material, namespace, keyname, name, model_data, glow, has_uuid) VALUES
    ('GRINDSTONE', 'core', 'resourceconverter', '<yellow>Resource Converter', 0, 0, 0)
ON CONFLICT (material, namespace, model_data) DO NOTHING;

INSERT INTO itemlore (item, priority, text)
VALUES ((SELECT id FROM items WHERE keyname = 'resourceconverter'), 0, '<gray>Can be used to exchange resources for other resources.')
ON CONFLICT (item, priority) DO NOTHING;