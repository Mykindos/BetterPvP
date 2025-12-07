package me.mykindos.betterpvp.core.block.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;

import static me.mykindos.betterpvp.core.Core.PACKAGE;

@Singleton
public class CoreBlockBootstrap {

    private final Core core;
    private final SmartBlockRegistry smartBlockRegistry;

    @Inject
    private CoreBlockBootstrap(Core core, SmartBlockRegistry smartBlockRegistry) {
        this.core = core;
        this.smartBlockRegistry = smartBlockRegistry;
    }

    public void register() {
        final Reflections reflections = new Reflections(PACKAGE);
        for (Class<? extends SmartBlock> clazz : reflections.getSubTypesOf(SmartBlock.class)) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            final SmartBlock block = core.getInjector().getInstance(clazz);
            smartBlockRegistry.registerBlock(block);
        }
    }

}
