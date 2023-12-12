create table if not exists shopitems
(
    id         int          auto_increment not null,
    Shopkeeper varchar(255) not null,
    Material   varchar(255) not null,
    ItemName   varchar(255) null,
    Data       int          null,
    MenuSlot   int          not null,
    MenuPage   int          null,
    Amount     int          not null,
    BuyPrice   int          not null,
    SellPrice  int          not null,
    constraint shopitems_id_pk
        primary key (id),
    constraint shopitems_shopkeeper_material_itemname_uk
        unique key (Shopkeeper, Material, ItemName)
);

create table if not exists shopitems_dynamic_pricing
(
    shopItemId    int         not null,
    MinSellPrice  int         not null,
    BaseSellPrice int         not null,
    MaxSellPrice  int         not null,
    MinBuyPrice   int         not null,
    BaseBuyPrice  int         not null,
    MaxBuyPrice   int         not null,
    BaseStock     int         not null,
    MaxStock      int         not null,
    CurrentStock  int         not null,
    constraint shopitems_dynamic_pricing_shopitems_id_fk
        foreign key (shopItemId) references shopitems (id)
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