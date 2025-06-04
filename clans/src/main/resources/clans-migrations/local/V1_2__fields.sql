create table if not exists clans_fields_ores
(
    world varchar(32) not null,
    x int not null,
    y int not null,
    z int not null,
    type varchar(32) not null,
    primary key (world, x, y, z)
);
