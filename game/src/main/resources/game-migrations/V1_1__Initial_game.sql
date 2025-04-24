-- Store hotbar layouts
CREATE TABLE IF NOT EXISTS champions_hotbar_layouts
(
    Gamer   varchar(255) not null,
    Role    varchar(20) not null,
    ID      int          not null,
    Slot    int          not null,
    Item    varchar(50) not null,
    PRIMARY KEY (Gamer, Role, ID, Slot)
);