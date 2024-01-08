create table if not exists logs
(
    id        int auto_increment primary key,
    Level     varchar(255)                          not null,
    Message   text                                  not null,
    Timestamp timestamp default current_timestamp() not null
);