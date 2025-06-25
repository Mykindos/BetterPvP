package me.mykindos.betterpvp.core.block;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.storage.DatabaseSmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.block.impl.CoreBlockBootstrap;
import me.mykindos.betterpvp.core.block.listener.SmartBlockChunkListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockListener;
import me.mykindos.betterpvp.core.block.listener.SmartBlockWorldListener;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;

public class SmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        if (Compatibility.TEXTURE_PROVIDER) {
            // Use NEXO implementation
            install(getNexoModule());
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
        bind(SmartBlockListener.class).asEagerSingleton();

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
}
