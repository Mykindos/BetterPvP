package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;

@Singleton
public class BrigadierChampionsCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierChampionsCommandLoader(Champions plugin) {
        super(plugin);
    }
}
