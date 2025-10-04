package me.mykindos.betterpvp.core.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeBootstrap;
import me.mykindos.betterpvp.core.block.impl.CoreBlockBootstrap;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeBootstrap;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.impl.CoreItemBootstrap;
import me.mykindos.betterpvp.core.metal.CastingMoldBootstrap;
import me.mykindos.betterpvp.core.metal.MetalBlockBootstrap;
import me.mykindos.betterpvp.core.metal.MetalItemBootstrap;
import me.mykindos.betterpvp.core.metal.MetalRecipeBootstrap;

public class CoreItemsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ItemRegistry.class).asEagerSingleton();

        bind(CoreItemBootstrap.class).asEagerSingleton();
        bind(CoreBlockBootstrap.class).asEagerSingleton();
        bind(MetalItemBootstrap.class).asEagerSingleton();
        bind(MetalBlockBootstrap.class).asEagerSingleton();
        bind(MetalRecipeBootstrap.class).asEagerSingleton();
        bind(CastingMoldBootstrap.class).asEagerSingleton();
        bind(ImbuementRecipeBootstrap.class).asEagerSingleton();
        bind(AnvilRecipeBootstrap.class).asEagerSingleton();
    }
}
