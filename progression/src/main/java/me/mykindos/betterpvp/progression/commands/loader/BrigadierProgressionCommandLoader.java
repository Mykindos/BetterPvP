package me.mykindos.betterpvp.progression.commands.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.progression.Progression;

@Singleton
public class BrigadierProgressionCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierProgressionCommandLoader(Progression plugin) {
        super(plugin);
    }
}
