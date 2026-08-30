-- =============================================================================
-- Grafana config table — generic flat key-value store for game-balance
-- configuration values read from YAML config files on server start.
--
-- Populated by GrafanaConfigSyncService (core) and contributing plugins on
-- every server start / reload. Source of truth is always the YAML config
-- files on disk.
--
-- realm        – realm ID (from Core.getCurrentRealm()) — isolates configs
--                per server/season pair so stale data never bleeds across
-- plugin       – Bukkit plugin name (e.g. "Core", "Champions", "Progression")
-- config_file  – YAML path relative to the plugin data folder, without .yml
--                (e.g. "items/weapon", "skills/skills", "roles")
-- config_key   – dot-separated leaf path within the YAML file
-- config_value – string representation of the value
-- =============================================================================

CREATE TABLE IF NOT EXISTS grafana_config (
    realm        INTEGER     NOT NULL,
    plugin       TEXT        NOT NULL,
    config_file  TEXT        NOT NULL,
    config_key   TEXT        NOT NULL,
    config_value TEXT        NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (realm, plugin, config_file, config_key)
);

CREATE INDEX IF NOT EXISTS idx_grafana_config_realm_plugin_file
    ON grafana_config (realm, plugin, config_file);
