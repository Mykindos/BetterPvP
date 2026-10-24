package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;

@Singleton
public class BrigadierCoreCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierCoreCommandLoader(Core plugin) {
        super(plugin);
    }
}
