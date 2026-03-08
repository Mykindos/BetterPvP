package me.mykindos.betterpvp.core.block;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.storage.DatabaseSmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.impl.CoreBlockBootstrap;
import me.mykindos.betterpvp.core.block.impl.DefaultSmartBlockFactory;
import me.mykindos.betterpvp.core.block.impl.DefaultSmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.listener.SmartBlockChunkListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockRemovalListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockTicker;
import me.mykindos.betterpvp.core.block.listener.SmartBlockWorldListener;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;

public class SmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        if (Compatibility.MODEL_ENGINE) {
            if (Compatibility.NEXO) {
                // Use NEXO implementation
                install(getNexoModule());
            } else if (Compatibility.ORAXEN) {
                // Use ORAXEN implementation
                install(getOraxenModule());
            } else {
                bind(SmartBlockFactory.class).to(DefaultSmartBlockFactory.class);
                bind(SmartBlockInteractionService.class).to(DefaultSmartBlockInteractionService.class);
            }
        } else {
            bind(SmartBlockFactory.class).to(DefaultSmartBlockFactory.class);
            bind(SmartBlockInteractionService.class).to(DefaultSmartBlockInteractionService.class);
        }

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

    @SneakyThrows
    private Module getNexoModule() {
        final Class<?> clazz = Class.forName("me.mykindos.betterpvp.core.block.nexo.NexoSmartBlockModule");
        final Object o = clazz.getConstructor().newInstance();
        if (o instanceof Module module) {
            return module;
        } else {
            throw new IllegalStateException("NexoSmartBlockModule is not a Module");
        }
    }

    @SneakyThrows
    private Module getOraxenModule() {
        final Class<?> clazz = Class.forName("me.mykindos.betterpvp.core.block.oraxen.OraxenSmartBlockModule");
        final Object o = clazz.getConstructor().newInstance();
        if (o instanceof Module module) {
            return module;
        } else {
            throw new IllegalStateException("OraxenSmartBlockModule is not a Module");
        }
    }
}
