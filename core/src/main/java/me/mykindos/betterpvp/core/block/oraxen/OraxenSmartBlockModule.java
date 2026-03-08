package me.mykindos.betterpvp.core.block.oraxen;

import com.google.inject.AbstractModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;

/**
 * Makes use of the Oraxen API to provide block-related services.
 */
@CustomLog
public class OraxenSmartBlockModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SmartBlockInteractionService.class).to(OraxenSmartBlockInteractionService.class).asEagerSingleton();
        bind(SmartBlockFactory.class).to(OraxenSmartBlockFactory.class).asEagerSingleton();

        log.info("Oraxen SmartBlock module loaded successfully").submit();
    }
}
