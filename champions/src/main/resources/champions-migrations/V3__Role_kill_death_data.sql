create table if not exists ${tablePrefix}killdeath_data
(
    Matchup varchar(255)  not null,
    Metric  varchar(255)  not null,
    Value   int default 0 null,
    constraint ${tablePrefix}killdeath_data_uk
    unique (Matchup, Metric)
);