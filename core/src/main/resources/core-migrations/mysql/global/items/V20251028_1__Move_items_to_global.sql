create table if not exists items
(
    id       int auto_increment
        primary key,
    Material   varchar(255) not null,
    Namespace  varchar(255) not null,
    Keyname    varchar(255) not null,
    Name       varchar(255) not null,
    ModelData  int          not null default 0,
    Glow       tinyint      not null,
    hasUUID    tinyint      not null default 0,
    constraint items_uk
        unique (Material, Namespace, ModelData)
);

create table if not exists itemdurability
(
    Item       int           not null,
    Durability int           not null,
    constraint itemdurability_item_uk
        unique (Item),
    constraint itemdurability___fk
        foreign key (Item) references items (id)
);

create table if not exists itemlore
(
    Item     int           not null,
    Priority int default 0 not null,
    Text     varchar(255)  not null,
    constraint itemlore_item_priority_uk
        unique (Item, Priority),
    constraint itemlore___fk
        foreign key (Item) references items (id)
);

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, ModelData, Glow, HasUUID) VALUES
    ('STONECUTTER', 'core', 'salvager', '<yellow>Salvager', 0, 0, 0);
INSERT IGNORE INTO itemlore
VALUES ((SELECT id FROM items WHERE Keyname = 'salvager'), 0, '<gray>Can be used to salvage weapons, tools, and armor.');

INSERT IGNORE INTO items (Material, Namespace, Keyname, Name, ModelData, Glow, HasUUID) VALUES
    ('GRINDSTONE', 'core', 'resourceconverter', '<yellow>Resource Converter', 0, 0, 0);
INSERT IGNORE INTO itemlore
VALUES ((SELECT id FROM items WHERE Keyname = 'resourceconverter'), 0, '<gray>Can be used to exchange resources for other resources.');