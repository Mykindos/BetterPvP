alter table clans_kills
    alter column killer_clan drop not null;

alter table clans_kills
    alter column victim_clan drop not null;

