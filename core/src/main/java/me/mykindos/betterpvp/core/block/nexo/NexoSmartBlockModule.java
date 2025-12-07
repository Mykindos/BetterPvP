package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;

/**
 * Makes use of the Nexo API to provide block-related services.
 */
@CustomLog
public class NexoSmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SmartBlockInteractionService.class).to(NexoSmartBlockInteractionService.class).asEagerSingleton();
        bind(SmartBlockFactory.class).to(NexoSmartBlockFactory.class).asEagerSingleton();

        log.info("Nexo SmartBlock module loaded successfully").submit();
    }
}
