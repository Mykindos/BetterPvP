alter table champions_combat_stats
    drop constraint champions_combat_stats_pk;

alter table champions_combat_stats
    add constraint champions_combat_stats_pk
        primary key (client, realm, class);