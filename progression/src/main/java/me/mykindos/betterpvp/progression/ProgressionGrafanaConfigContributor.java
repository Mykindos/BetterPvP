package me.mykindos.betterpvp.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.stats.GrafanaConfigSyncService;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

/**
 * Progression-specific contributor to the {@code grafana_config} database table.
 *
 * <p>Delegates all database work to {@link GrafanaConfigSyncService} (core)
 * and contributes all {@code items/*.yml} files (excluding {@code recipes.yml}).
 *
 * <p>Registered as a {@link Reloadable} via {@link Progression#onEnable()};
 * fires on server start and on every {@code /progression reload}.
 */
@Singleton
@CustomLog
public class ProgressionGrafanaConfigContributor implements Reloadable {

    private static final String[] ITEM_CONFIGS = {
            "items/armor", "items/block", "items/consumable",
            "items/material", "items/misc", "items/tool", "items/weapon"
    };

    private final GrafanaConfigSyncService grafanaConfigSyncService;
    private final Progression progression;

    @Inject
    public ProgressionGrafanaConfigContributor(GrafanaConfigSyncService grafanaConfigSyncService,
                                               Progression progression) {
        this.grafanaConfigSyncService = grafanaConfigSyncService;
        this.progression = progression;
    }

    /**
     * Enqueues async syncs for all Progression {@code items/*} config contributions.
     * Called on server start and on every {@code /progression reload}.
     */
    @Override
    public void reload() {
        for (String itemConfig : ITEM_CONFIGS) {
            grafanaConfigSyncService.syncYamlConfig(progression, itemConfig);
        }
    }
}
