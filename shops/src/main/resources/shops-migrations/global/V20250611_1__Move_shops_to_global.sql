create table if not exists shopitems
(
    id         int auto_increment not null,
    Shopkeeper varchar(255)       not null,
    Material   varchar(255)       not null,
    ItemName   varchar(255)       null,
    ModelData  int                null,
    MenuSlot   int                not null,
    MenuPage   int                null,
    Amount     int                not null,
    BuyPrice   int                not null,
    SellPrice  int                not null,
    constraint shopitems_id_pk
        primary key (id),
    constraint shopitems_shopkeeper_material_itemname_uk
        unique key (Shopkeeper, Material, ItemName)
);

create table if not exists shopitems_dynamic_pricing
(
    shopItemId    int          not null,
    Server        varchar(255) not null,
    Season        varchar(255) not null,
    MinSellPrice  int          not null,
    BaseSellPrice int          not null,
    MaxSellPrice  int          not null,
    MinBuyPrice   int          not null,
    BaseBuyPrice  int          not null,
    MaxBuyPrice   int          not null,
    BaseStock     int          not null,
    MaxStock      int          not null,
    CurrentStock  int          not null,
    CreatedTime   timestamp default current_timestamp,
    UpdatedTime   timestamp default current_timestamp on update current_timestamp,
    unique key (shopItemId, Server, Season),
    constraint shopitems_dynamic_pricing_shopitems_id_fk
        foreign key (shopItemId) references shopitems (id),
    check (MinSellPrice <= BaseSellPrice),
    check (BaseSellPrice <= MaxSellPrice),
    check (MinBuyPrice <= BaseBuyPrice),
    check (BaseBuyPrice <= MaxBuyPrice)

);

create table if not exists shopitems_flags
(
    id              int auto_increment primary key,
    shopItemId      int          not null,
    PersistentKey   varchar(255) not null,
    PersistentValue varchar(255) not null,
    constraint shopitems_flags_shopitems_id_fk
        foreign key (shopItemId) references shopitems (id)
);

create table if not exists auctions
(
    id          varchar(36)         not null
        primary key,
    Server      varchar(255)        not null,
    Season      varchar(255)        not null,
    Gamer       varchar(36)         not null,
    Item        text                not null,
    Price       int                 not null,
    Expiry      bigint              not null,
    Sold        tinyint   default 0 not null,
    Cancelled   tinyint   default 0 not null,
    Delivered   tinyint   default 0 not null,
    CreatedTime timestamp default current_timestamp,
    UpdatedTime timestamp default current_timestamp on update current_timestamp
);

create index auctions_server_season_gamer_index on auctions (Server, Season, Gamer);

create table if not exists auction_transaction_history
(
    AuctionId varchar(36) not null
        primary key,
    Buyer     varchar(36) not null,
    TimeSold  timestamp   not null,
    constraint auction_transaction_history_auctions_id_fk
        foreign key (AuctionId) references auctions (id)
);

