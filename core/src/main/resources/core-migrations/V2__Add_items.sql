create table ${tablePrefix}items
(
    id       int auto_increment
        primary key,
    Material varchar(255) not null,
    Module   varchar(255) not null,
    Name     varchar(255) not null,
    Glow     tinyint      not null,
    constraint items_uk
        unique (Material, Module)
);

create table ${tablePrefix}itemlore
(
    Item     int           not null,
    Priority int default 0 not null,
    Text     varchar(255)  not null,
    constraint itemlore___fk
        foreign key (Item) references ${tablePrefix}items (id)
);
