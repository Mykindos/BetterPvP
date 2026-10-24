package me.mykindos.betterpvp.shops.commands.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.shops.Shops;

@Singleton
public class BrigadierShopsCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierShopsCommandLoader(Shops plugin) {
        super(plugin);
    }
}
