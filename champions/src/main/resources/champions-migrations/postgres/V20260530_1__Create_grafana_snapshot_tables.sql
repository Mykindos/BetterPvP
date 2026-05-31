-- Grafana snapshot tables for Champions analytics dashboards.
-- Rows are inserted periodically by the Champions plugin (GrafanaSnapshotRepository).
-- Each snapshot captures cumulative realm totals at that point in time,
-- enabling both "latest state" queries and historical trend analysis in Grafana.

CREATE TABLE IF NOT EXISTS grafana_role_matchup_snapshot (
    id          BIGSERIAL     PRIMARY KEY,
    realm       INTEGER       NOT NULL,
    captured_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    role        VARCHAR(100)  NOT NULL,
    vs_role     VARCHAR(100)  NOT NULL,
    kills       BIGINT        NOT NULL DEFAULT 0,
    deaths      BIGINT        NOT NULL DEFAULT 0,
    kdr         NUMERIC(10,2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_role_matchup_realm_ts
    ON grafana_role_matchup_snapshot (realm, captured_at DESC);

CREATE TABLE IF NOT EXISTS grafana_role_playtime_snapshot (
    id             BIGSERIAL     PRIMARY KEY,
    realm          INTEGER       NOT NULL,
    captured_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    role           VARCHAR(100)  NOT NULL,
    kills          BIGINT        NOT NULL DEFAULT 0,
    deaths         BIGINT        NOT NULL DEFAULT 0,
    kdr            NUMERIC(10,2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_role_playtime_realm_ts
    ON grafana_role_playtime_snapshot (realm, captured_at DESC);

CREATE TABLE IF NOT EXISTS grafana_skill_kdr_snapshot (
    id             BIGSERIAL     PRIMARY KEY,
    realm          INTEGER       NOT NULL,
    captured_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    skill_name     VARCHAR(200)  NOT NULL,
    kills          BIGINT        NOT NULL DEFAULT 0,
    deaths         BIGINT        NOT NULL DEFAULT 0,
    kdr            NUMERIC(10,2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_skill_kdr_realm_ts
    ON grafana_skill_kdr_snapshot (realm, captured_at DESC);

