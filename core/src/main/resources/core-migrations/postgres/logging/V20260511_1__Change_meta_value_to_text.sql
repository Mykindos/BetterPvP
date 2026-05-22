alter table world_logs_metadata
    alter column meta_value type text using meta_value::text;

drop index if exists world_logs_metadata_key_value_index;
drop index if exists world_logs_metadata_value_index;

create index world_logs_metadata_key_value_index
    on world_logs_metadata (realm, meta_key, (left(meta_value, 200)));

create index world_logs_metadata_value_index
    on world_logs_metadata (realm, (left(meta_value, 200)));