
create table if not exists gamer_properties
(
    Gamer    varchar(36)      not null,
    Server   tinyint unsigned not null,
    Season   tinyint unsigned not null,
    Property varchar(255)     not null,
    Value    varchar(255)     null,
    primary key (Gamer, Server, Season, Property)
);
