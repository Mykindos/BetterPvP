package me.mykindos.betterpvp.core.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

/**
 * Core's contributor to the {@code grafana_config} database table.
 *
 * <p>Syncs all {@code items/*.yml} files (excluding {@code recipes.yml})
 * owned by the Core plugin on server start and on every {@code /core reload}.
 *
 * <p>Registered as a {@link Reloadable} via {@code Core#onEnable()}.
 */
@Singleton
@CustomLog
public class CoreGrafanaConfigContributor implements Reloadable {

    private static final String[] ITEM_CONFIGS = {
            "items/armor", "items/block", "items/consumable",
            "items/material", "items/misc", "items/tool", "items/weapon"
    };

    private final GrafanaConfigSyncService grafanaConfigSyncService;
    private final Core core;

    @Inject
    public CoreGrafanaConfigContributor(GrafanaConfigSyncService grafanaConfigSyncService, Core core) {
        this.grafanaConfigSyncService = grafanaConfigSyncService;
        this.core = core;
    }

    @Override
    public void reload() {
        for (String itemConfig : ITEM_CONFIGS) {
            grafanaConfigSyncService.syncYamlConfig(core, itemConfig);
        }
    }
}

