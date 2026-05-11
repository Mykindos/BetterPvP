alter table world_logs_metadata
    alter column meta_value type text using meta_value::text;