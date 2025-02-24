create table if not exists client_name_history
(
    Client   varchar(36)                         not null,
    Name     varchar(255)                        not null,
    LastSeen TIMESTAMP default CURRENT_TIMESTAMP not null,
    constraint client_name_history_pk
        primary key (Client, Name)
);
