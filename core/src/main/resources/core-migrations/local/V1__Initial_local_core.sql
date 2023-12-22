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
        unique (Material, Namespace)
);

create table if not exists itemdurability
(
    Item       int           not null,
    Durability int           not null,
    constraint itemdurability___fk,
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


create table if not exists gamer_properties
(
    Gamer    varchar(255) not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    primary key (Gamer, Property)
);
