package me.mykindos.betterpvp.core.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GRAFANA_CONFIG;

/**
 * Core infrastructure for syncing YAML game-configuration values into the
 * {@code grafana_config} database table so that Grafana dashboards always
 * reflect the live server config.
 *
 * <p>The table is a generic flat key-value store keyed by
 * {@code (plugin, config_file, config_key)}. Every leaf node in a YAML config
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
     * {@code (plugin.getName(), configPath, key)}.
     *
     * @param plugin     the plugin that owns the config file
     * @param configPath config path relative to the plugin data folder,
     *                   without the {@code .yml} extension
     *                   (e.g. {@code "items/material"})
     */
    public void syncYamlConfig(BPvPPlugin plugin, String configPath) {
        final String pluginName = plugin.getName();
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ExtendedYamlConfiguration config = plugin.getConfig(configPath);
            int count = 0;
            for (String key : config.getKeys(true)) {
                if (config.isConfigurationSection(key)) continue;
                Object value = config.get(key);
                if (value == null) continue;
                upsert(ctx, pluginName, configPath, key, String.valueOf(value));
                count++;
            }
            log.info("grafana_config: synced {} keys from {}/{}.yml", count, pluginName, configPath).submit();
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
        database.getAsyncDslContext().executeAsyncVoid(ctx ->
                upsert(ctx, pluginName, configFile, configKey, value)
        ).exceptionally(ex -> {
            log.error("Failed to sync grafana_config raw entry [{}/{}/{}]", pluginName, configFile, configKey, ex).submit();
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void upsert(DSLContext ctx, String plugin, String configFile, String configKey, String configValue) {
        ctx.insertInto(GRAFANA_CONFIG)
                .set(GRAFANA_CONFIG.PLUGIN, plugin)
                .set(GRAFANA_CONFIG.CONFIG_FILE, configFile)
                .set(GRAFANA_CONFIG.CONFIG_KEY, configKey)
                .set(GRAFANA_CONFIG.CONFIG_VALUE, configValue)
                .set(GRAFANA_CONFIG.UPDATED_AT, DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE))
                .onConflict(GRAFANA_CONFIG.PLUGIN, GRAFANA_CONFIG.CONFIG_FILE, GRAFANA_CONFIG.CONFIG_KEY)
                .doUpdate()
                .set(GRAFANA_CONFIG.CONFIG_VALUE, configValue)
                .set(GRAFANA_CONFIG.UPDATED_AT, DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE))
                .execute();
    }
}