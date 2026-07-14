package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.stats.GrafanaConfigSyncService;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

/**
 * Champions-specific contributor to the {@code grafana_config} database table.
 *
 * <p>Delegates all database work to {@link GrafanaConfigSyncService} (core)
 * and contributes the following config files:
 * <ul>
 *   <li>All {@code items/*.yml} except {@code recipes.yml}</li>
 *   <li>{@code skills/skills.yml} – skill cooldown / energy / balance params</li>
 * </ul>
 *
 * <p>Role base-health values are a special case: they come from the
 * {@link Role} enum (not a YAML file) and are written under
 * {@code config_file = "roles"}, {@code config_key = "<role>.base_health"}.
 *
 * <p>Called once from {@link Champions#onEnable()} after items and skills
 * have been fully loaded, and again on {@code /champions reload}.
 */
@Singleton
@CustomLog
public class ChampionsGrafanaConfigContributor implements Reloadable {

    private static final String[] ITEM_CONFIGS = {
            "items/armor", "items/block", "items/consumable",
            "items/material", "items/misc", "items/tool", "items/weapon"
    };

    private final GrafanaConfigSyncService grafanaConfigSyncService;
    private final Champions champions;

    @Inject
    public ChampionsGrafanaConfigContributor(GrafanaConfigSyncService grafanaConfigSyncService,
                                             Champions champions) {
        this.grafanaConfigSyncService = grafanaConfigSyncService;
        this.champions = champions;
    }

    /**
     * Enqueues async syncs for all Champions config contributions.
     * Called on server start and on every {@code /champions reload}.
     */
    @Override
    public void reload() {
        for (String itemConfig : ITEM_CONFIGS) {
            grafanaConfigSyncService.syncYamlConfig(champions, itemConfig);
        }
        grafanaConfigSyncService.syncYamlConfig(champions, "skills/skills");
        syncRoleHealth();
    }

    /**
     * Writes each {@link Role}'s base health into {@code grafana_config}
     * under {@code config_file = "roles"},
     * {@code config_key = "<role_name_lower>.base_health"}.
     */
    private void syncRoleHealth() {
        for (Role role : Role.values()) {
            grafanaConfigSyncService.syncRaw(
                    champions,
                    "roles",
                    role.getName().toLowerCase(java.util.Locale.ROOT) + ".base_health",
                    String.valueOf(role.getHealth()));
        }
    }
}

