alter table clans_fields_ores
    modify world varchar(64) not null;

alter table clans_fields_ores
    modify type varchar(64) not null;

alter table clans_fields_ores
    add data varchar(255) null AFTER type;
