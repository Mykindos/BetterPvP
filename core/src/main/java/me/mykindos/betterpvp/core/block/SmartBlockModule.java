package me.mykindos.betterpvp.core.block;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.block.impl.CoreBlockBootstrap;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;

public class SmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        if (Compatibility.NEXO) {
            // Use NEXO implementation
            install(getNexoModule());

            requireBinding(SmartBlockInteractionService.class);
            requireBinding(SmartBlockFactory.class);
        }

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
