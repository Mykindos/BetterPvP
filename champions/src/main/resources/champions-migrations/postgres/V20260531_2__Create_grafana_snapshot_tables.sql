-- Grafana snapshot tables for Champions analytics.
-- Each row is an hourly point-in-time capture of cumulative realm totals,
-- allowing Grafana to show both current state (latest snapshot) and trends (time-series).
--
-- Role matchup KDR is intentionally excluded: it can be derived live from
-- kills + champions_kills using the existing time column.

CREATE TABLE IF NOT EXISTS grafana_role_playtime_snapshot
(
    id             BIGSERIAL PRIMARY KEY,
    realm          INTEGER        NOT NULL,
    captured_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    role           TEXT           NOT NULL,
    kills          BIGINT         NOT NULL DEFAULT 0,
    deaths         BIGINT         NOT NULL DEFAULT 0,
    kdr            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_grafana_role_playtime_realm_time
    ON grafana_role_playtime_snapshot (realm, captured_at DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS grafana_skill_kdr_snapshot
(
    id             BIGSERIAL PRIMARY KEY,
    realm          INTEGER        NOT NULL,
    captured_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    skill_name     TEXT           NOT NULL,
    kills          BIGINT         NOT NULL DEFAULT 0,
    deaths         BIGINT         NOT NULL DEFAULT 0,
    kdr            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_grafana_skill_kdr_realm_time
    ON grafana_skill_kdr_snapshot (realm, captured_at DESC);

