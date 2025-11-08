
-- Store hotbar layouts
CREATE /*stop jooq*/ TABLE IF NOT EXISTS champions_hotbar_layouts
(
    client  BIGINT REFERENCES clients (id) ON DELETE CASCADE,
    role    varchar(20) not null,
    id      int          not null,
    slot    int          not null,
    item    varchar(50) not null,
    PRIMARY KEY (client, role, id, slot)
);

INSERT INTO property_map (property, type) VALUES ('CHAMPIONS_WINS', 'int')
ON CONFLICT (property, type) DO NOTHING;