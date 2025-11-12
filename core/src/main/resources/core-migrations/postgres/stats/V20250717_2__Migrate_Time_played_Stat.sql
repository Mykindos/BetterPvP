INSERT INTO client_stats (Client, Period, Statname, Stat)
Select Client, 'Legacy' as Period, 'TIME_PLAYED' as Statname, CAST(Value as DECIMAL) FROM client_properties
WHERE Property = 'TIME_PLAYED';