create table if not exists kills
(
    Id           varchar(36) primary key,
    Server       varchar(64) not null,
    Season       varchar(64) not null,
    Killer       varchar(36) not null,
    Victim       varchar(36) not null,
    Contribution float       not null,
    Damage       float       not null,
    RatingDelta  int         not null,
    Time         bigint      not null,
    Valid        tinyint     not null default 1
);

create table if not exists kill_contributions
(
    KillId       varchar(36) not null,
    Id           varchar(36) not null,
    Contributor  varchar(36) not null,
    Contribution float       not null,
    Damage       float       not null,
    primary key (Id),
    unique (KillId, Contributor),
    constraint kills_Id_fk
        foreign key (KillId) references kills (Id)
);

create table if not exists combat_stats
(
    Client            varchar(36)  not null,
    Server            varchar(255) not null,
    Season            varchar(255) not null,
    Rating            int          not null,
    Killstreak        int          not null default 0,
    HighestKillstreak int          not null default 0,
    Valid             tinyint      not null default 1,
    primary key (Client, Server)

);

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36), serverVar VARCHAR(255), seasonVar VARCHAR(255))
BEGIN
    SELECT IFNULL(SUM(kills_count), 0)   AS Kills,
           IFNULL(SUM(deaths_count), 0)  AS Deaths,
           IFNULL(SUM(assists_count), 0) AS Assists,
           MAX(cs.Rating)                AS Rating,
           MAX(cs.Killstreak)            AS Killstreak,
           MAX(cs.HighestKillstreak)     AS HighestKillstreak
    FROM combat_stats AS cs
             LEFT JOIN (SELECT Killer, COUNT(*) AS kills_count
                        FROM kills
                        GROUP BY Killer) AS k ON cs.Client = k.Killer
             LEFT JOIN (SELECT Victim, COUNT(*) AS deaths_count
                        FROM kills
                        GROUP BY Victim) AS d ON cs.Client = d.Victim
             LEFT JOIN (SELECT Contributor, COUNT(*) AS assists_count
                        FROM kill_contributions
                        GROUP BY Contributor) AS ac ON cs.Client = ac.Contributor
    WHERE cs.Client = player
      AND Server = serverVar
      AND Season = seasonVar
      AND cs.Valid = 1;
END;

CREATE INDEX idx_kills_killer ON kills (Server, Season, Killer);
CREATE INDEX idx_kills_victim ON kills (Server, Season, Victim);
CREATE INDEX idx_kills_killer_victim ON kills (Server, Season, Killer, Victim);
CREATE INDEX idx_kill_contributions_contributor ON kill_contributions (Contributor);