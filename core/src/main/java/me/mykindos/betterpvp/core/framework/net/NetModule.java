package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Wiring for the network layer. Every seam comes from {@link NetworkLayer}, which is the one thing that decides what
 * carries messages, holds shared state and moves players.
 */
public class NetModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    MessageBus bus(NetworkLayer layer) {
        return layer.bus();
    }

    @Provides
    @Singleton
    SiteDirectory directory(NetworkLayer layer) {
        return layer.directory();
    }

    @Provides
    @Singleton
    PlayerTransfer transfer(NetworkLayer layer) {
        return layer.transfer();
    }
}
