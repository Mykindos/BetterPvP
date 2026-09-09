package me.mykindos.betterpvp.core.world.site;

import com.google.inject.AbstractModule;

/**
 * Wiring for the site framework. This is where the single-server and networked implementations are chosen between,
 * and the only place that choice is made.
 */
public class SiteModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Placement.class).toProvider(PlacementProvider.class);
    }
}
