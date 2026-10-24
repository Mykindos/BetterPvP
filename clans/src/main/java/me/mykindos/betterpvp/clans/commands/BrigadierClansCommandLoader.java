package me.mykindos.betterpvp.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;

@Singleton
public class BrigadierClansCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierClansCommandLoader(Clans plugin) {
        super(plugin);
    }
}
