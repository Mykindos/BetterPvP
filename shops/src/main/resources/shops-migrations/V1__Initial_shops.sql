create table if not exists ${tablePrefix}shopitems
(
    id         varchar(36)  not null,
    Shopkeeper varchar(255) not null,
    Material   varchar(255) not null,
    ItemName   varchar(255) null,
    Data       int          null,
    MenuSlot   int          not null,
    MenuPage   int          null,
    Amount     int          not null,
    BuyPrice   int          not null,
    SellPrice  int          not null,
    constraint ${tablePrefix}shopitems_id_pk
        primary key (id)
);

create table if not exists ${tablePrefix}shopitems_dynamic_pricing
(
    shopItemId    varchar(36) not null,
    MinSellPrice  int         not null,
    BaseSellPrice int         not null,
    MaxSellPrice  int         not null,
    MinBuyPrice   int         not null,
    BaseBuyPrice  int         not null,
    MaxBuyPrice   int         not null,
    BaseStock     int         not null,
    MaxStock      int         not null,
    CurrentStock  int         not null,
    constraint ${tablePrefix}shopitems_dynamic_pricing_shopitems_id_fk
        foreign key (shopItemId) references ${tablePrefix}shopitems (id)
);

create table if not exists ${tablePrefix}shopitems_flags
(
    id         int auto_increment primary key,
    shopItemId varchar(36)  not null,
    Flag       varchar(255) not null,
    constraint ${tablePrefix}shopitems_flags_shopitems_id_fk
        foreign key (shopItemId) references ${tablePrefix}shopitems (id)
);