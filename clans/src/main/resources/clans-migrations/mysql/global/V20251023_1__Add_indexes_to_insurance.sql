create index clan_insurance_Clan_index
    on clan_insurance (Clan);

create index clan_insurance_Clan_Time_index
    on clan_insurance (Clan, Time desc);

create index clan_insurance_Time_index
    on clan_insurance (Time desc);