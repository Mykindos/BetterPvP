
create table if not exists progression_farming
(
    id varchar(36) not null primary key,
    Gamer varchar(36) not null,
    Material varchar(50) not null,
    Location varchar(285) not null,
    ActionType varchar(10) not null,
    YieldLevel varchar(5) not null,
    timestamp timestamp default now() not null
);

CREATE INDEX idx_progression_farming_timestamp ON progression_farming (timestamp);
CREATE INDEX idx_progression_farming_gamer ON progression_farming (Gamer);
CREATE INDEX idx_progression_farming_composite ON progression_farming (timestamp, Gamer);