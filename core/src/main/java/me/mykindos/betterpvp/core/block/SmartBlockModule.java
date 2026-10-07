package me.mykindos.betterpvp.core.block;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.block.custom.PackSmartBlockFactory;
import me.mykindos.betterpvp.core.block.custom.PackSmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.storage.DatabaseSmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.impl.CoreBlockBootstrap;
import me.mykindos.betterpvp.core.block.listener.SmartBlockChunkListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockRemovalListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockTicker;
import me.mykindos.betterpvp.core.block.listener.SmartBlockWorldListener;

public class SmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SmartBlockFactory.class).to(PackSmartBlockFactory.class);
        bind(SmartBlockInteractionService.class).to(PackSmartBlockInteractionService.class);
        requireBinding(SmartBlockInteractionService.class);
        requireBinding(SmartBlockFactory.class);
        requireBinding(SmartBlockDataStorage.class);

        // Bind register persistence
        bind(SmartBlockDataStorage.class).to(DatabaseSmartBlockDataStorage.class).asEagerSingleton();
        bind(SmartBlockDataManager.class).asEagerSingleton();

        // Register listeners
        bind(SmartBlockWorldListener.class).asEagerSingleton();
        bind(SmartBlockChunkListener.class).asEagerSingleton();
        bind(SmartBlockRemovalListener.class).asEagerSingleton();
        bind(SmartBlockTicker.class).asEagerSingleton();

        // Register core blocks
        bind(CoreBlockBootstrap.class).asEagerSingleton();
    }

}
