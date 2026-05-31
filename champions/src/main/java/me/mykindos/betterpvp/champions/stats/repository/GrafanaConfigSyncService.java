package me.mykindos.betterpvp.champions.stats.repository;

/**
 * @deprecated Replaced by {@link ChampionsGrafanaConfigContributor}.
 *             The shared infrastructure now lives in
 *             {@link me.mykindos.betterpvp.core.stats.GrafanaConfigSyncService} (core).
 */
@Deprecated
public class GrafanaConfigSyncService extends ChampionsGrafanaConfigContributor {

    @com.google.inject.Inject
    public GrafanaConfigSyncService(me.mykindos.betterpvp.core.stats.GrafanaConfigSyncService delegate,
                                    me.mykindos.betterpvp.champions.Champions champions) {
        super(delegate, champions);
    }
}

