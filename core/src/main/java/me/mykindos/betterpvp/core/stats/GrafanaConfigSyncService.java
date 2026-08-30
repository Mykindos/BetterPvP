package me.mykindos.betterpvp.core.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.util.LinkedHashMap;
import java.util.Map;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GRAFANA_CONFIG;

/**
 * Core infrastructure for syncing YAML game-configuration values into the
 * {@code grafana_config} database table so that Grafana dashboards always
 * reflect the live server config.
 *
 * <p>The table is a generic flat key-value store keyed by
 * {@code (realm, plugin, config_file, config_key)}. Every leaf node in a YAML config
 * is walked and upserted as a single row; intermediate section keys are skipped.
 *
 * <p>Usage — each plugin contributes its own configs:
 * <pre>{@code
 * // Core (items/material.yml):
 * grafanaConfigSyncService.syncYamlConfig(corePlugin, "items/material");
 *
 * // Champions (weapon, armor, skills YAMLs):
 * grafanaConfigSyncService.syncYamlConfig(championsPlugin, "items/weapon");
 * grafanaConfigSyncService.syncYamlConfig(championsPlugin, "items/armor");
 * grafanaConfigSyncService.syncYamlConfig(championsPlugin, "skills/skills");
 *
 * // Ad-hoc value not backed by a YAML file:
 * grafanaConfigSyncService.syncRaw(championsPlugin, "roles", "knight.base_health", "20.0");
 * }</pre>
 */
@Singleton
@CustomLog
public class GrafanaConfigSyncService {

    private final Database database;

    @Inject
    public GrafanaConfigSyncService(Database database) {
        this.database = database;
    }

    /**
     * Asynchronously walks every leaf key in the YAML config located at
     * {@code configPath} (relative to the given plugin's data folder) and
     * upserts each one into {@code grafana_config} keyed by
     * {@code (realm, plugin.getName(), configPath, key)}.
     *
     * <p>Stale rows (keys present in the DB but absent from the current YAML)
     * are deleted in the same transaction as the upserts, so readers always
     * observe a consistent snapshot of the config file.
     *
     * @param plugin     the plugin that owns the config file
     * @param configPath config path relative to the plugin data folder,
     *                   without the {@code .yml} extension
     *                   (e.g. {@code "items/material"})
     */
    public void syncYamlConfig(BPvPPlugin plugin, String configPath) {
        final String pluginName = plugin.getName();
        final int realmId = Core.getCurrentRealm().getId();
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ExtendedYamlConfiguration config = plugin.getConfig(configPath);
            Map<String, String> liveEntries = new LinkedHashMap<>();
            for (String key : config.getKeys(true)) {
                if (config.isConfigurationSection(key)) continue;
                Object value = config.get(key);
                if (value == null) continue;
                liveEntries.put(key, String.valueOf(value));
            }

            if (liveEntries.isEmpty()) {
                // Guard: an empty result likely means the file failed to load.
                // Skip the sync entirely rather than deleting all rows for this file.
                log.warn("grafana_config: {}/{}.yml yielded no keys — skipping sync to avoid accidental data loss", pluginName, configPath).submit();
                return;
            }

            ctx.transaction(trx -> {
                DSLContext trxCtx = DSL.using(trx);
                for (Map.Entry<String, String> entry : liveEntries.entrySet()) {
                    upsert(trxCtx, realmId, pluginName, configPath, entry.getKey(), entry.getValue());
                }
                int deleted = trxCtx.deleteFrom(GRAFANA_CONFIG)
                        .where(GRAFANA_CONFIG.REALM.eq(realmId))
                        .and(GRAFANA_CONFIG.PLUGIN.eq(pluginName))
                        .and(GRAFANA_CONFIG.CONFIG_FILE.eq(configPath))
                        .and(GRAFANA_CONFIG.CONFIG_KEY.notIn(liveEntries.keySet()))
                        .execute();
                if (deleted > 0) {
                    log.info("grafana_config: removed {} stale keys from {}/{}.yml", deleted, pluginName, configPath).submit();
                }
            });

            log.info("grafana_config: synced {} keys from {}/{}.yml", liveEntries.size(), pluginName, configPath).submit();
        }).exceptionally(ex -> {
            log.error("Failed to sync grafana_config for {}/{}.yml", pluginName, configPath, ex).submit();
            return null;
        });
    }

    /**
     * Asynchronously upserts a single raw value into {@code grafana_config}.
     * Use this for values that do not come from a YAML config file
     * (e.g. enum constants such as Role base health).
     *
     * @param plugin     the owning plugin
     * @param configFile logical source label stored in the {@code config_file} column
     * @param configKey  dot-separated key stored in the {@code config_key} column
     * @param value      string representation of the value
     */
    public void syncRaw(BPvPPlugin plugin, String configFile, String configKey, String value) {
        final String pluginName = plugin.getName();
        final int realmId = Core.getCurrentRealm().getId();
        database.getAsyncDslContext().executeAsyncVoid(ctx ->
                upsert(ctx, realmId, pluginName, configFile, configKey, value)
        ).exceptionally(ex -> {
            log.error("Failed to sync grafana_config raw entry [{}/{}/{}]", pluginName, configFile, configKey, ex).submit();
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void upsert(DSLContext ctx, int realmId, String plugin, String configFile, String configKey, String configValue) {
        ctx.insertInto(GRAFANA_CONFIG)
                .set(GRAFANA_CONFIG.REALM, realmId)
                .set(GRAFANA_CONFIG.PLUGIN, plugin)
                .set(GRAFANA_CONFIG.CONFIG_FILE, configFile)
                .set(GRAFANA_CONFIG.CONFIG_KEY, configKey)
                .set(GRAFANA_CONFIG.CONFIG_VALUE, configValue)
                .set(GRAFANA_CONFIG.UPDATED_AT, DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE))
                .onConflict(GRAFANA_CONFIG.REALM, GRAFANA_CONFIG.PLUGIN, GRAFANA_CONFIG.CONFIG_FILE, GRAFANA_CONFIG.CONFIG_KEY)
                .doUpdate()
                .set(GRAFANA_CONFIG.CONFIG_VALUE, configValue)
                .set(GRAFANA_CONFIG.UPDATED_AT, DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE))
                .execute();
    }
}
