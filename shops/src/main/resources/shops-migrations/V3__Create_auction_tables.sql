create table if not exists auctions
(
    id        varchar(36)       not null
        primary key,
    Gamer     varchar(36)       not null,
    Item      text              not null,
    Price     int               not null,
    Expiry    bigint            not null,
    Sold      tinyint default 0 not null,
    Cancelled tinyint default 0 not null,
    Delivered tinyint default 0 not null
);

create table if not exists auction_transaction_history
(
    AuctionId varchar(36) not null
        primary key,
    Buyer     varchar(36) not null,
    TimeSold  timestamp   not null,
    constraint auction_transaction_history_auctions_id_fk
        foreign key (AuctionId) references auctions (id)
);

