INSERT INTO client_stats (Client, Period, StatType, Stat)
Select Client, 'Legacy' as Period, 'TIME_PLAYED' as StatTYPE, CAST(Value as BIGINT) FROM client_properties
WHERE Property = 'TIME_PLAYED';